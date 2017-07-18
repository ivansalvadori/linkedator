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

public class LinkedatorScenario3Test {

    ModelBasedLinkedator linkedator;

    @Before
    public void configureSc3() throws IOException {
        linkedator = new ModelBasedLinkedator();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario3/domainOntology.owl")) {
            RDFDataMgr.read(linkedator.getOntologies(), in, Lang.RDFXML);
        }

        String descriptionContent1 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfAmericaDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription1 = new Gson().fromJson(descriptionContent1, SemanticMicroserviceDescription.class);
        semanticMicroserviceDescription1.setIpAddress("192.168.10.1");
        semanticMicroserviceDescription1.setServerPort("8080");
        semanticMicroserviceDescription1.setUriBase("/service/");
        linkedator.register(semanticMicroserviceDescription1);

        String descriptionContent2 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfEuropeDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription2 = new Gson().fromJson(descriptionContent2, SemanticMicroserviceDescription.class);
        semanticMicroserviceDescription2.setIpAddress("192.168.10.2");
        semanticMicroserviceDescription2.setServerPort("8080");
        semanticMicroserviceDescription2.setUriBase("/service/");
        linkedator.register(semanticMicroserviceDescription2);

        String descriptionContent3 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfAsiaDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription3 = new Gson().fromJson(descriptionContent3, SemanticMicroserviceDescription.class);
        semanticMicroserviceDescription3.setIpAddress("192.168.10.3");
        semanticMicroserviceDescription3.setServerPort("8080");
        semanticMicroserviceDescription3.setUriBase("/service/");
        linkedator.register(semanticMicroserviceDescription3);

        String descriptionContent4 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfJapanDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription4 = new Gson().fromJson(descriptionContent4, SemanticMicroserviceDescription.class);
        semanticMicroserviceDescription4.setIpAddress("192.168.10.4");
        semanticMicroserviceDescription4.setServerPort("8080");
        semanticMicroserviceDescription4.setUriBase("/service/");
        linkedator.register(semanticMicroserviceDescription4);

        String descriptionContent5 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfEmiratesDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription5 = new Gson().fromJson(descriptionContent5, SemanticMicroserviceDescription.class);
        semanticMicroserviceDescription5.setIpAddress("192.168.10.5");
        semanticMicroserviceDescription5.setServerPort("8080");
        semanticMicroserviceDescription5.setUriBase("/service/");
        linkedator.register(semanticMicroserviceDescription5);

    }

    @Test
    public void mustCreateMultipleInferredLinkInPersonForMultipleBanks() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario3/person.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }

        linkedator.createLinks(model, new NullLinkVerifier());
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {" +
                "  <http://10.1.1.1/people-microservice/13579> ssp:hasBankAccount ?europe, ?asia, ?america, ?japan, ?emirates.\n" +
                "  ?europe   a ssp:BankAccount; owl:sameAs <http://192.168.10.2:8080/service/bankOfEurope/13579>.\n" +
                "  ?asia     a ssp:BankAccount; owl:sameAs <http://192.168.10.3:8080/service/bankOfAsia/13579>.\n" +
                "  ?america  a ssp:BankAccount; owl:sameAs <http://192.168.10.1:8080/service/bankOfAmerica/13579>.\n" +
                "  ?japan    a ssp:BankAccount; owl:sameAs <http://192.168.10.4:8080/service/bankOfJapan/13579>.\n" +
                "  ?emirates a ssp:BankAccount; owl:sameAs <http://192.168.10.5:8080/service/bankOfEmirates/13579>.\n" +
                "}\n", model).execAsk());
    }
}
