package br.ufsc.inf.lapesd.linkedator;

import br.ufsc.inf.lapesd.linkedator.links.LinkVerifier;
import br.ufsc.inf.lapesd.linkedator.templates.UriTemplateIndex;
import org.apache.jena.rdf.model.*;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.shared.JenaException;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ModelBasedLinkedator {
    private Model ontologies = ModelFactory.createDefaultModel();
    private ReasonerBox reasonerBox = new ReasonerBox();
    private UriTemplateIndex uriTemplateIndex = new UriTemplateIndex();
    private static final Logger logger = LoggerFactory.getLogger(ModelBasedLinkedator.class);


    public void createLinks(Model model, LinkVerifier linkVerifier) {
        //TODO catch JenaExceptions, do not commit modifications to model, and throw Informative exception
        InfModel infModel = ModelFactory.createInfModel(getReasoner().bind(model.getGraph()));
        for (ResIterator it = infModel.listSubjects(); it.hasNext(); ) {
            Resource subject = it.next();
            addInferredTypes(subject, model);
            createExplicitLinks(subject, model, linkVerifier);
            createInferredLinks(subject, model, linkVerifier);
        }
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

    public void registerDescription(SemanticMicroserviceDescription smd) {
        uriTemplateIndex.register(smd);
    }

    @Nonnull
    public Reasoner getReasoner() {
        return reasonerBox.get();
    }

    public void setReasoner(@Nonnull Reasoner reasoner) {
        this.reasonerBox.set(reasoner);
    }

    @Nonnull
    public Model getOntologies() {
        return ontologies;
    }

    private class ReasonerBox {
        private Reasoner reasoner;

        @Nonnull
        public synchronized Reasoner get() {
            if (reasoner == null) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try (InputStream in = cl.getResourceAsStream("linkedator/default.rules");
                     BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    List<Rule> rules = Rule.parseRules(Rule.rulesParserFromReader(reader));
                    GenericRuleReasoner gReasoner = new GenericRuleReasoner(rules);
                    gReasoner.setMode(GenericRuleReasoner.FORWARD);
                    reasoner = gReasoner.bindSchema(ontologies.getGraph());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return reasoner;
        }

        public synchronized void set(@Nonnull Reasoner reasoner) {
            this.reasoner = reasoner;
        }
    }
}
