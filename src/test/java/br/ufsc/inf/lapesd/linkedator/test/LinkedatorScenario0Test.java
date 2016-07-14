package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.linkedator.Linkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

public class LinkedatorScenario0Test {

    Linkedator linkedador;

    public void addMicroserviceDescription(SemanticMicroserviceDescription semanticMicroserviceDescription) {
        linkedador.registryDescription(semanticMicroserviceDescription);
    }

    @Before
    public void configure() throws IOException {

        String ontology = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/domainOntology.owl"), "UTF-8");
        linkedador = new Linkedator(ontology);

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(policeReportDescription);

    }

    @Test
    public void mustCreateExplicitLinkInPoliceRepor() throws IOException {
        String policeReport = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/policeReport.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(policeReport);
        System.out.println(linkedRepresentation);
        String expectedLink = "\"http://ssp-ontology.com#victim\":{\"http://ssp-ontology.com#numeroRg\":\"123456\",\"@type\":\"http://schema.org/Person\",\"http://www.w3.org/2002/07/owl#sameAs\":\"10.1.1.1/vitima/123456\"}";
        Assert.assertTrue(linkedRepresentation.contains(expectedLink));
    }

    @Test
    public void mustCreateInferredLinkInPerson() throws IOException {
        String person = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/person.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(person);
        System.out.println(linkedRepresentation);
        String expectedLinked = "\"http://ssp-ontology.com#envolvedIn\":{\"@type\":\"http://ssp-ontology.com#PoliceReport\",\"http://www.w3.org/2002/07/owl#sameAs\":\"10.1.1.2/reports/13579\"}";
        Assert.assertTrue(linkedRepresentation.contains(expectedLinked));
    }
}
