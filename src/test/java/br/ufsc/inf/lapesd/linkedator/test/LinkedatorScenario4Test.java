package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.linkedator.ObjectPropertyBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

public class LinkedatorScenario4Test {

    ObjectPropertyBasedLinkedator linkedador;

    public void addMicroserviceDescription(SemanticMicroserviceDescription semanticMicroserviceDescription) {
        linkedador.registryDescription(semanticMicroserviceDescription);
    }

    @Before
    public void configure() throws IOException {

        String ontology = IOUtils.toString(this.getClass().getResourceAsStream("/scenario4/domainOntology.owl"), "UTF-8");
        linkedador = new ObjectPropertyBasedLinkedator(ontology);

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario4/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        microservicesDescription.setIpAddress("192.168.10.1");
        microservicesDescription.setServerPort("8080");
        microservicesDescription.setUriBase("/service/");
        linkedador.registryDescription(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario4/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        policeReportDescription.setIpAddress("192.168.10.2");
        policeReportDescription.setServerPort("8080");
        policeReportDescription.setUriBase("/service/");
        linkedador.registryDescription(policeReportDescription);

    }

    @Test
    public void mustCreateExplicitLinkInPoliceRepor() throws IOException {
        String policeReport = IOUtils.toString(this.getClass().getResourceAsStream("/scenario4/policeReportArray.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(policeReport, false);
        System.out.println(linkedRepresentation);
        String expectedLink = "\"http://ssp-ontology.com#victim\":{\"http://ssp-ontology.com#numeroRg\":\"123456\",\"@type\":\"http://schema.org/Person\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.1:8080/service/vitima/123456\"}";
        Assert.assertTrue(linkedRepresentation.contains(expectedLink));

        String expectedLink2 = "http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.1:8080/service/vitima/654321";
        Assert.assertTrue(linkedRepresentation.contains(expectedLink2));

        String expectedLink3 = "http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.1:8080/service/vitima/6758493";
        Assert.assertTrue(linkedRepresentation.contains(expectedLink3));

        String expectedLink4 = "http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.1:8080/service/vitima/4433221111";
        Assert.assertTrue(linkedRepresentation.contains(expectedLink4));
    }

    @Test
    public void mustCreateInferredLinkInPerson() throws IOException {
        String person = IOUtils.toString(this.getClass().getResourceAsStream("/scenario4/personArray.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(person, false);
        System.out.println(linkedRepresentation);
        String expectedLinked = "\"http://ssp-ontology.com#envolvedIn\":{\"@type\":\"http://ssp-ontology.com#PoliceReport\",\"http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.2:8080/service/reports/13579\"}";
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked));

        String expectedLinked1 = "http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.2:8080/service/reports/5555";
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked1));

        String expectedLinked2 = "http://www.w3.org/2002/07/owl#sameAs\":\"http://192.168.10.2:8080/service/reports/666";
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked2));
    }

}
