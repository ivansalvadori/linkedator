package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.linkedator.Linkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

public class LinkedatorScenario3Test extends LinkedatorScenario2Test {

    Linkedator linkedador;

    @Before
    public void configureSc3() throws IOException {

        String ontology = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/domainOntology.owl"), "UTF-8");
        linkedador = new Linkedator(ontology);

        String descriptionContent1 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfAmericaDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription1 = new Gson().fromJson(descriptionContent1, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(semanticMicroserviceDescription1);
        semanticMicroserviceDescription1.setIpAddress("192.168.10.1");
        semanticMicroserviceDescription1.setServerPort("8080");
        semanticMicroserviceDescription1.setUriBase("/service/");

        String descriptionContent2 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfEuropeDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription2 = new Gson().fromJson(descriptionContent2, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(semanticMicroserviceDescription2);
        semanticMicroserviceDescription2.setIpAddress("192.168.10.2");
        semanticMicroserviceDescription2.setServerPort("8080");
        semanticMicroserviceDescription2.setUriBase("/service/");

        String descriptionContent3 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfAsiaDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription3 = new Gson().fromJson(descriptionContent3, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(semanticMicroserviceDescription3);
        semanticMicroserviceDescription3.setIpAddress("192.168.10.3");
        semanticMicroserviceDescription3.setServerPort("8080");
        semanticMicroserviceDescription3.setUriBase("/service/");

        String descriptionContent4 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfJapanDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription4 = new Gson().fromJson(descriptionContent4, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(semanticMicroserviceDescription4);
        semanticMicroserviceDescription4.setIpAddress("192.168.10.4");
        semanticMicroserviceDescription4.setServerPort("8080");
        semanticMicroserviceDescription4.setUriBase("/service/");

        String descriptionContent5 = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/microserviceBankOfEmiratesDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription semanticMicroserviceDescription5 = new Gson().fromJson(descriptionContent5, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(semanticMicroserviceDescription5);
        semanticMicroserviceDescription5.setIpAddress("192.168.10.5");
        semanticMicroserviceDescription5.setServerPort("8080");
        semanticMicroserviceDescription5.setUriBase("/service/");

    }

    @Test
    public void mustCreateMultipleInferredLinkInPersonForMultipleBanks() throws IOException {
        String person = IOUtils.toString(this.getClass().getResourceAsStream("/scenario3/person.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(person);
        System.out.println(linkedRepresentation);
        String expectedLinked1 = "http://ssp-ontology.com#hasBankAccount";
        String expectedLinked2 = "{\"@type\":\"http://ssp-ontology.com#BankAccount\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http:192.168.10.1:8080/service/bankOfAmerica/13579\"}";
        String expectedLinked3 = "{\"@type\":\"http://ssp-ontology.com#BankAccount\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http:192.168.10.2:8080/service/bankOfEurope/13579\"}";
        String expectedLinked4 = "{\"@type\":\"http://ssp-ontology.com#BankAccount\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http:192.168.10.3:8080/service/bankOfAsia/13579\"}";
        String expectedLinked6 = "{\"@type\":\"http://ssp-ontology.com#BankAccount\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http:192.168.10.5:8080/service/bankOfEmirates/13579\"}";
        String expectedLinked5 = "{\"@type\":\"http://ssp-ontology.com#BankAccount\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http:192.168.10.4:8080/service/bankOfJapan/13579\"}";

        Assert.assertTrue(linkedRepresentation.contains(expectedLinked1));
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked2));
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked3));
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked4));
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked5));
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked6));
    }
}
