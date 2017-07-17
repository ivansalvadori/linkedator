package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;
import java.io.InputStream;

import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.links.LinkVerifier;
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

import br.ufsc.inf.lapesd.linkedator.ObjectPropertyBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.OntologyReader;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

public class RemoveInvalidLinkTest {

    private class MyVerifier implements LinkVerifier {
        @Override
        public boolean verify(String link) {
            String expectedLink1 = "http://192.168.10.1:8080/service/vitima?x=123456&y=88888";
            String expectedLink2 = "http://192.168.10.2:8080/service/reports/13579";
            return !link.equalsIgnoreCase(expectedLink1) && !link.equalsIgnoreCase(expectedLink2);
        }
    }

    private ModelBasedLinkedator linkedator;

    @Before
    public void configure() throws IOException {
        linkedator = new ModelBasedLinkedator();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario0/domainOntology.owl")) {
            RDFDataMgr.read(linkedator.getOntologies(), in, Lang.RDFXML);
        }

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        microservicesDescription.setIpAddress("192.168.10.1");
        microservicesDescription.setServerPort("8080");
        microservicesDescription.setUriBase("/service/");
        linkedator.registerDescription(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        policeReportDescription.setIpAddress("192.168.10.2");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");
        linkedator.registerDescription(policeReportDescription);

    }

    @Test
    public void mustRemoveInvalidLinkDirect() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario0/policeReport.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new MyVerifier());

        Assert.assertFalse(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/123> ssp:victim/owl:sameAs ?x.\n" +
                "}", model).execAsk());
        /* type assertion should still be added */
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/123> ssp:victim/rdf:type sch:Person.\n" +
                "}", model).execAsk());
    }

    @Test
    public void mustRemoveInvalidLinkInverse() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario0/person.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new MyVerifier());

        Assert.assertFalse(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE + "ASK WHERE {\n" +
                "  <http://10.1.1.1/people-microservice/13579> ssp:envolvedIn ?x.\n" +
                "}", model).execAsk());
    }

}
