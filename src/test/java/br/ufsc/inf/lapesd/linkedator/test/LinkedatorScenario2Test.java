package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.linkedator.Linkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

public class LinkedatorScenario2Test {

    Linkedator linkedador;

    @Before
    public void configureSc2() throws IOException {

        String ontology = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/domainOntology.owl"), "UTF-8");
        linkedador = new Linkedator(ontology);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(policeReportDescription);
        policeReportDescription.setIpAddress("192.168.10.1");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");

        String vehicleDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/microserviceOfVehicleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription vehicleDescription = new Gson().fromJson(vehicleDescriptionContent, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(vehicleDescription);
        vehicleDescription.setIpAddress("192.168.10.2");
        vehicleDescription.setServerPort("8080");
        vehicleDescription.setUriBase("/service/");

        String imobiliaryDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/microserviceOfImobiliaryDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription imobiliaryDescription = new Gson().fromJson(imobiliaryDescriptionContent, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(imobiliaryDescription);
        imobiliaryDescription.setIpAddress("192.168.10.3");
        imobiliaryDescription.setServerPort("8080");
        imobiliaryDescription.setUriBase("/service/");

        String bankAccountDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/microserviceOfBankAccountDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription bankAccountDescription = new Gson().fromJson(bankAccountDescriptionContent, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(bankAccountDescription);
        bankAccountDescription.setIpAddress("192.168.10.4");
        bankAccountDescription.setServerPort("8080");
        bankAccountDescription.setUriBase("/service/");

    }

    @Test
    public void mustCreateMultipleInferredLinkInPersonForMultipleOwner() throws IOException {
        String person = IOUtils.toString(this.getClass().getResourceAsStream("/scenario2/person.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(person, false);
        System.out.println(linkedRepresentation);
        String expectedLinked1 = "\"http://ssp-ontology.com#envolvedIn\":{\"@type\":\"http://ssp-ontology.com#PoliceReport\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.1:8080/service/reports/13579\"}";
        String expectedLinked2 = "{\"@type\":\"http://ssp-ontology.com#BankAccount\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.4:8080/service/reports/4444\"}";
        String expectedLinked3 = "{\"@type\":\"http://ssp-ontology.com#Vehicle\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.2:8080/service/reports/13579\"}";
        String expectedLinked4 = "{\"@type\":\"http://ssp-ontology.com#Imobiliary\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.3:8080/service/reports/13579\"}";

        Assert.assertTrue(linkedRepresentation.contains(expectedLinked1));
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked2));
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked3));
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked4));
    }

}
