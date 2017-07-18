package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;
import java.io.InputStream;

import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.links.LinkVerifier;
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

import static br.ufsc.inf.lapesd.linkedator.test.TestUtils.SPARQL_PROLOGUE;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

public class LinkedatorScenario2Test {

    ModelBasedLinkedator linkedator;
    LinkVerifier verifier;

    @Before
    public void configureSc2() throws IOException {
        verifier = new NullLinkVerifier();
        linkedator = new ModelBasedLinkedator();
        try (InputStream in = getClass().getResourceAsStream("/scenario2/domainOntology.owl")) {
            RDFDataMgr.read(linkedator.getOntologies(), in, Lang.RDFXML);
        }

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        policeReportDescription.setIpAddress("192.168.10.1");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");
        linkedator.register(policeReportDescription);

        String vehicleDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/microserviceOfVehicleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription vehicleDescription = new Gson().fromJson(vehicleDescriptionContent, SemanticMicroserviceDescription.class);
        vehicleDescription.setIpAddress("192.168.10.2");
        vehicleDescription.setServerPort("8080");
        vehicleDescription.setUriBase("/service/");
        linkedator.register(vehicleDescription);

        String imobiliaryDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/microserviceOfImobiliaryDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription imobiliaryDescription = new Gson().fromJson(imobiliaryDescriptionContent, SemanticMicroserviceDescription.class);
        imobiliaryDescription.setIpAddress("192.168.10.3");
        imobiliaryDescription.setServerPort("8080");
        imobiliaryDescription.setUriBase("/service/");
        linkedator.register(imobiliaryDescription);

        String bankAccountDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/microserviceOfBankAccountDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription bankAccountDescription = new Gson().fromJson(bankAccountDescriptionContent, SemanticMicroserviceDescription.class);
        bankAccountDescription.setIpAddress("192.168.10.4");
        bankAccountDescription.setServerPort("8080");
        bankAccountDescription.setUriBase("/service/");
        linkedator.register(bankAccountDescription);

    }

    @Test
    public void mustCreateMultipleInferredLinkInPersonForMultipleOwner() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/scenario2/person.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, verifier);

        Assert.assertTrue(QueryExecutionFactory.create(
                SPARQL_PROLOGUE +
                        "ASK WHERE {\n" +
                        "  ?p ssp:envolvedIn ?r.\n" +
                        "  ?r a ssp:PoliceReport.\n" +
                        "  ?r owl:sameAs <http://192.168.10.1:8080/service/reports/13579>.\n" +
                        "}", model).execAsk());
        Assert.assertTrue(QueryExecutionFactory.create(
                SPARQL_PROLOGUE +
                        "ASK WHERE {\n" +
                        "  ?x a ssp:BankAccount.\n" +
                        "  ?x owl:sameAs <http://192.168.10.4:8080/service/reports/4444>.\n" +
                        "}", model).execAsk());
        Assert.assertTrue(QueryExecutionFactory.create(
                SPARQL_PROLOGUE +
                        "ASK WHERE {\n" +
                        "  ?x a ssp:Vehicle.\n" +
                        "  ?x owl:sameAs <http://192.168.10.2:8080/service/reports/13579>.\n" +
                        "}", model).execAsk());
        Assert.assertTrue(QueryExecutionFactory.create(
                SPARQL_PROLOGUE +
                        "ASK WHERE {\n" +
                        "  ?x a ssp:Imobiliary.\n" +
                        "  ?x owl:sameAs <http://192.168.10.3:8080/service/reports/13579>.\n" +
                        "}", model).execAsk());
    }

}
