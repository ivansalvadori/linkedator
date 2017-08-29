package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;
import java.io.InputStream;

import br.ufsc.inf.lapesd.linkedator.Linkedator;
import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

public class LinkedatorScenario1Test {

    Linkedator linkedator;

    @Before
    public void initSc1() throws IOException {
        linkedator = TestUtils.createLinkedator(getClass().getResourceAsStream(
                "/scenario1/domainOntology.owl"), Lang.RDFXML);

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        microservicesDescription.setIpAddress("192.168.10.1");
        microservicesDescription.setServerPort("8080");
        microservicesDescription.setUriBase("/service/");
        linkedator.register(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        policeReportDescription.setIpAddress("192.168.10.2");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");
        linkedator.register(policeReportDescription);

        String animalReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfAnimalDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription animalReportDescription = new Gson().fromJson(animalReportDescriptionContent, SemanticMicroserviceDescription.class);
        animalReportDescription.setIpAddress("192.168.10.3");
        animalReportDescription.setServerPort("8080");
        animalReportDescription.setUriBase("/service/");
        linkedator.register(animalReportDescription);
    }

    @Test
    public void mustCreateExplicitArrayOfVictimLinkInPoliceRepor() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario1/policeReportArrayVictim.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE + "" +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/123> ssp:victim ?v1, ?v2, ?v3.\n" +
                "  ?v1 a sch:Person. ?v2 a sch:Person. ?v3 a sch:Person.\n" +
                "  ?v1 ssp:numeroRg \"123456\". ?v2 ssp:numeroRg \"44444\". ?v3 ssp:numeroRg \"5555\".\n" +
                "  ?v1 owl:sameAs <http://192.168.10.1:8080/service/vitima/123456>.\n" +
                "  ?v2 owl:sameAs <http://192.168.10.1:8080/service/vitima/44444>.\n" +
                "  ?v3 owl:sameAs <http://192.168.10.1:8080/service/vitima/5555>.\n" +
                "}", model).execAsk());
    }

    @Test
    public void mustCreateExplicitArrayLinkInPoliceReporPersonAndAnimal() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario1/policeReportPersonAndAnimalVictims.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/123> ssp:victim ?v1, ?v2, ?v3.\n" +
                "  ?v1 a sch:Person. ?v2 a sch:Animal. ?v3 a sch:Animal.\n" +
                "  ?v1 ssp:numeroRg \"123456\".\n" +
                "  ?v2 ssp:petTrackId \"0001\". ?v3 ssp:petTrackId \"0002\".\n" +
                "  ?v1 owl:sameAs <http://192.168.10.1:8080/service/vitima/123456>.\n" +
                "  ?v2 owl:sameAs <http://192.168.10.3:8080/service/pet/0001>.\n" +
                "  ?v3 owl:sameAs <http://192.168.10.3:8080/service/pet/0002>.\n" +
                "}", model).execAsk());
    }
}
