package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;
import java.io.InputStream;

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

public class LinkedatorScenario4Test {

    ModelBasedLinkedator linkedator;

    @Before
    public void configure() throws IOException {

        linkedator = new ModelBasedLinkedator();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario4/domainOntology.owl")) {
            RDFDataMgr.read(linkedator.getOntologies(), in, Lang.RDFXML);
        }

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario4/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        microservicesDescription.setIpAddress("192.168.10.1");
        microservicesDescription.setServerPort("8080");
        microservicesDescription.setUriBase("/service/");
        linkedator.register(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario4/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        policeReportDescription.setIpAddress("192.168.10.2");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");
        linkedator.register(policeReportDescription);

    }

    @Test
    public void mustCreateExplicitLinkInPoliceRepor() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario4/policeReportArray.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/123> ssp:victim ?v1.\n" +
                "  ?v1 a sch:Person.\n" +
                "  ?v1 owl:sameAs <http://192.168.10.1:8080/service/vitima/123456>.\n" +
                "}", model).execAsk());
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/456> ssp:victim ?v1.\n" +
                "  ?v1 a sch:Person.\n" +
                "  ?v1 owl:sameAs <http://192.168.10.1:8080/service/vitima/654321>.\n" +
                "}", model).execAsk());
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/789> ssp:victim ?v1, ?v2.\n" +
                "  ?v1 a sch:Person.\n" +
                "  ?v1 owl:sameAs <http://192.168.10.1:8080/service/vitima/6758493>.\n" +
                "  ?v2 a sch:Person.\n" +
                "  ?v2 owl:sameAs <http://192.168.10.1:8080/service/vitima/4433221111>.\n" +
                "}", model).execAsk());
    }

    @Test
    public void mustCreateInferredLinkInPerson() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario4/personArray.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }

        linkedator.createLinks(model, new NullLinkVerifier());
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASk WHERE {\n" +
                "  <http://10.1.1.1/people-microservice/13579> ssp:envolvedIn ?r.\n" +
                "  ?r a ssp:PoliceReport;\n" +
                "     owl:sameAs <http://192.168.10.2:8080/service/reports/13579>.\n" +
                "}", model).execAsk());
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASk WHERE {\n" +
                "  <http://10.1.1.1/people-microservice/5555> ssp:envolvedIn ?r.\n" +
                "  ?r a ssp:PoliceReport;\n" +
                "     owl:sameAs <http://192.168.10.2:8080/service/reports/5555>.\n" +
                "}", model).execAsk());
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASk WHERE {\n" +
                "  <http://10.1.1.1/people-microservice/666> ssp:envolvedIn ?r.\n" +
                "  ?r a ssp:PoliceReport;\n" +
                "     owl:sameAs <http://192.168.10.2:8080/service/reports/666>.\n" +
                "}", model).execAsk());
    }

}
