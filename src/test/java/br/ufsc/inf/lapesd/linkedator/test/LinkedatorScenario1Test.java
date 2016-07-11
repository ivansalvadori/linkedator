package br.ufsc.inf.lapesd.linkedator.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.ufsc.inf.lapesd.linkedator.Linkedador;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;

public class LinkedatorScenario1Test extends LinkedatorScenario0Test {

    Linkedador linkedador;

    @Before
    public void initSc1() throws IOException {
        String ontology = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/domainOntology.owl"), "UTF-8");
        linkedador = new Linkedador(ontology);

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(policeReportDescription);

        String animalReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfAnimalDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription animalReportDescription = new Gson().fromJson(animalReportDescriptionContent, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(animalReportDescription);
    }

    @Test
    public void mustCreateExplicitArrayOfVictimLinkInPoliceRepor() throws IOException {
        String policeReport = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/policeReportArrayVictim.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(policeReport);
        System.out.println(linkedRepresentation);
        String expectedVictim1 = "{\"http://ssp-ontology.com#numeroRg\":\"123456\",\"@type\":\"http://schema.org/Person\",\"http://www.w3.org/2000/01/rdf-schema#seeAlso\":\"10.1.1.1/vitima/123456\"}";
        String expectedVictim2 = "{\"http://ssp-ontology.com#numeroRg\":\"44444\",\"@type\":\"http://schema.org/Person\",\"http://www.w3.org/2000/01/rdf-schema#seeAlso\":\"10.1.1.1/vitima/44444\"}";
        String expectedVictim3 = "{\"http://ssp-ontology.com#numeroRg\":\"5555\",\"@type\":\"http://schema.org/Person\",\"http://www.w3.org/2000/01/rdf-schema#seeAlso\":\"10.1.1.1/vitima/5555\"}";
        Assert.assertTrue(linkedRepresentation.contains(expectedVictim1));
        Assert.assertTrue(linkedRepresentation.contains(expectedVictim2));
        Assert.assertTrue(linkedRepresentation.contains(expectedVictim3));
    }

    @Test
    public void mustCreateExplicitArrayLinkInPoliceReporPersonAndAnimal() throws IOException {
        String policeReport = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/policeReportPersonAndAnimalVictims.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(policeReport);
        System.out.println(linkedRepresentation);
        String expectedVictim1 = "{\"http://ssp-ontology.com#numeroRg\":\"123456\",\"@type\":\"http://schema.org/Person\",\"http://www.w3.org/2000/01/rdf-schema#seeAlso\":\"10.1.1.1/vitima/123456\"}";
        String expectedVictim2 = "{\"http://ssp-ontology.com#petTrackId\":\"0001\",\"@type\":\"http://schema.org/Animal\",\"http://www.w3.org/2000/01/rdf-schema#seeAlso\":\"10.1.1.3/pet/0001\"}";
        String expectedVictim3 = "{\"http://ssp-ontology.com#petTrackId\":\"0002\",\"@type\":\"http://schema.org/Animal\",\"http://www.w3.org/2000/01/rdf-schema#seeAlso\":\"10.1.1.3/pet/0002\"}";
        Assert.assertTrue(linkedRepresentation.contains(expectedVictim1));
        Assert.assertTrue(linkedRepresentation.contains(expectedVictim2));
        Assert.assertTrue(linkedRepresentation.contains(expectedVictim3));
    }

}
