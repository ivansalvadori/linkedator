package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;
import java.io.InputStream;

import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.links.LinkVerifier;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

public class LinkedatorScenario0Test {

    private ModelBasedLinkedator linkedator;
    private LinkVerifier verifier;


    @Before
    public void configure() throws IOException {
        verifier = new NullLinkVerifier();
        linkedator = new ModelBasedLinkedator();
        try (InputStream in = getClass().getResourceAsStream("/scenario0/domainOntology.owl")) {
            RDFDataMgr.read(linkedator.getOntologies(), in, Lang.RDFXML);
        }

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        microservicesDescription.setIpAddress("192.168.10.1");
        microservicesDescription.setServerPort("8080");
        microservicesDescription.setUriBase("/service/");
        linkedator.register(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        policeReportDescription.setIpAddress("192.168.10.2");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");
        linkedator.register(policeReportDescription);

    }

    @Test
    public void mustCreateExplicitLinkInPoliceReport() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/scenario0/policeReport.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }

        linkedator.createLinks(model, verifier);
        Resource r = model.createResource("http://10.1.1.2/policeReport-microservice/123");
        r = r.getPropertyResourceValue(createProperty("http://ssp-ontology.com#victim"));
        Assert.assertTrue(r.hasProperty(OWL2.sameAs, createResource(
                "http://192.168.10.1:8080/service/vitima?x=123456&y=88888")));
        Assert.assertTrue(r.hasProperty(RDF.type,
                createResource("http://schema.org/Person")));
    }

    @Test
    public void mustCreateInferredLinkInPerson() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/scenario0/person.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, verifier);

        Resource r = model.createResource("http://10.1.1.1/people-microservice/13579");
        Statement s = r.getProperty(createProperty("http://ssp-ontology.com#envolvedIn"));
        Assert.assertNotNull(s);
        Assert.assertTrue(s.getObject().isResource());
        r = s.getObject().asResource();
        Assert.assertTrue(r.hasProperty(RDF.type,
                createResource("http://ssp-ontology.com#PoliceReport")));
        Assert.assertTrue(r.hasProperty(OWL2.sameAs,
                createResource("http://192.168.10.2:8080/service/reports/13579")));
    }
}
