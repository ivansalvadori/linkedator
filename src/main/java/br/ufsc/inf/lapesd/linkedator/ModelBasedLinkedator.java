package br.ufsc.inf.lapesd.linkedator;

import br.ufsc.inf.lapesd.linkedator.links.LinkVerifier;
import br.ufsc.inf.lapesd.linkedator.templates.UriTemplateIndex;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class ModelBasedLinkedator implements Linkedator {
    private ReadWriteLock ontologiesLock = new ReentrantReadWriteLock();
    private Model ontologiesRaw = ModelFactory.createDefaultModel();
    private InfModel ontologies;
    private Reasoner ontologiesReasoner, dataReasoner;
    private UriTemplateIndex uriTemplateIndex = new UriTemplateIndex();
    private final String tboxFile = "linkedator/tbox.rules";
    private final String aboxFile = "linkedator/abox.rules";

    public ModelBasedLinkedator() {
        ontologiesReasoner = createReasoner(tboxFile, aboxFile);
        ontologies = ModelFactory.createInfModel(ontologiesReasoner, ontologiesRaw);
        dataReasoner = createReasoner(aboxFile);
    }

    private GenericRuleReasoner createReasoner(String... rulesFiles) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        List<Rule> rules = new ArrayList<>();
        for (String rulesFile : rulesFiles) {
            try (InputStream in = cl.getResourceAsStream(rulesFile);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                rules.addAll(Rule.parseRules(Rule.rulesParserFromReader(reader)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        GenericRuleReasoner gReasoner = new GenericRuleReasoner(rules);
        gReasoner.setMode(GenericRuleReasoner.FORWARD);
        return gReasoner;
    }

    @Override
    public void createLinks(@Nonnull Model model, @Nonnull LinkVerifier linkVerifier) {
        ontologiesLock.readLock().lock();
        try {
            Model out = ModelFactory.createDefaultModel();
            InfModel infModel = ModelFactory.createInfModel(dataReasoner, model);
            for (ResIterator it = model.listSubjects(); it.hasNext(); ) {
                Resource subject = it.next();
                subject = subject.isAnon()
                        ? infModel.createResource(subject.getId())
                        : infModel.createResource(subject.getURI());
                addInferredTypes(subject, out);
                createExplicitLinks(subject, out, linkVerifier);
                createInferredLinks(subject, out, linkVerifier);
            }
            model.add(out); //only add links if successful
        } catch (Exception e) {
            throw new LinkCreationException(e);
        } finally {
            ontologiesLock.readLock().unlock();
        }
    }

    @Nonnull
    @Override
    public Model getOntologies() {
        ontologiesLock.readLock().lock();
        try {
            Model copy = ModelFactory.createDefaultModel();
            copy.add(ontologies);
            return copy;
        } finally {
            ontologiesLock.readLock().unlock();
        }
    }

    @Override
    public void updateOntologies(@Nonnull Model model) {
        ontologiesLock.writeLock().lock();
        try {
            ontologiesRaw.removeAll().add(model);
            reasonOnOntologies();
        } finally {
            ontologiesLock.writeLock().unlock();
        }
    }

    @Override
    public void addToOntologies(@Nonnull Model model) {
        ontologiesLock.writeLock().lock();
        try {
            ontologiesRaw.add(model);
            reasonOnOntologies();
        } finally {
            ontologiesLock.writeLock().unlock();
        }
    }

    private void reasonOnOntologies() {
        ontologies.rebind();
        ontologies.prepare();
        dataReasoner = createReasoner(aboxFile).bindSchema(ontologies.getGraph());
    }


    @Override
    public void register(@Nonnull SemanticMicroserviceDescription smd) {
        uriTemplateIndex.register(smd);
    }

    private void addInferredTypes(Resource subject, Model out) {
        for (StmtIterator it = subject.listProperties(RDF.type); it.hasNext(); ) {
            out.add(subject, RDF.type, it.next().getResource());
        }
    }

    private void createExplicitLinks(Resource subject, Model out, LinkVerifier linkVerifier) {
        uriTemplateIndex.streamAny().usingShallowPropertiesOf(subject)
                .filter(m -> !subject.equals(m.getResource()))
                .filter(m -> linkVerifier.verify(m.getUri()))
                .forEach(m -> {
                    Property link = subject.hasProperty(RDF.type, m.getType())
                            ? OWL2.sameAs : RDFS.seeAlso;
                    out.add(subject, link, out.createResource(m.getUri()));
                });
    }

    private void createInferredLinks(Resource subject, Model out, LinkVerifier linkVerifier) {
        Set<Property> properties = subject.listProperties(RDF.type).toList().stream()
                .map(Statement::getResource)
                .flatMap(t -> ontologies.listSubjectsWithProperty(RDFS.domain, t).toList().stream())
                .filter(t -> t.hasProperty(RDFS.range))
                .map(r -> r.as(Property.class))
                .collect(Collectors.toSet());
        for (Property property : properties) {
            for (StmtIterator it = property.listProperties(RDFS.range); it.hasNext(); ) {
                Resource range = it.next().getResource();
                Resource subjectType = property.getPropertyResourceValue(RDFS.domain);
                uriTemplateIndex.streamWithType(range).usingShallowPropertiesOf(subject)
                        .filter(m -> !m.getResource().equals(subject))
                        .filter(m -> linkVerifier.verify(m.getUri()))
                        .forEach(m -> {
                            out.add(subject, RDF.type, subjectType);
                            out.add(subject, property, out.createResource()
                                    .addProperty(RDF.type, range)
                                    .addProperty(OWL2.sameAs, m.getResource()));
                        });
            }
        }
    }
}
