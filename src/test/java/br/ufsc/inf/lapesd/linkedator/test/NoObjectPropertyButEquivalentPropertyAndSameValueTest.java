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

public class NoObjectPropertyButEquivalentPropertyAndSameValueTest {

    ModelBasedLinkedator linkedator;

    @Before
    public void configure() throws IOException {
        linkedator = new ModelBasedLinkedator();
        try (InputStream in = this.getClass().getResourceAsStream("/noObjectPropertyEquivalentPropertyAndSameValue/mergedDomainOntology.owl")) {
            RDFDataMgr.read(linkedator.getOntologies(), in, Lang.RDFXML);
        }

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/noObjectPropertyEquivalentPropertyAndSameValue/microserviceOfPeopleDescription.jsonld"),
                "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        microservicesDescription.setIpAddress("192.168.10.1");
        microservicesDescription.setServerPort("8080");
        microservicesDescription.setUriBase("/service/");
        linkedator.register(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/noObjectPropertyEquivalentPropertyAndSameValue/microserviceOfPoliceReportDescription.jsonld"),
                "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        policeReportDescription.setIpAddress("192.168.10.2");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");
        linkedator.register(policeReportDescription);

    }

    @Test
    public void mustCreateExplicitLinkInPoliceRepor() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/noObjectPropertyEquivalentPropertyAndSameValue/policeReport.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE + "" +
                "ASK WHERE{\n" +
                "  <http://192.168.10.2/service/report/123> pol:victim ?v.\n" +
                "  ?v rdfs:seeAlso <http://192.168.10.2:8080/service/reports/123>,\n" +
                "                  <http://192.168.10.1:8080/service/vitima?x=123&y=456>.\n" +
                "}", model).execAsk());
    }

    @Test
    public void mustCreateReverseLinksInPerson() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/noObjectPropertyEquivalentPropertyAndSameValue/person.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new NullLinkVerifier());
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE + "" +
                "ASK WHERE{\n" +
                "  <http://192.168.10.1/service/vitima?x=123&y=456> rdfs:seeAlso <http://192.168.10.2:8080/service/reports/123>.\n" +
                "}", model).execAsk());
    }
}
