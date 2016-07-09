package br.com.srs.linkedator.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;

import br.com.srs.linkedator.Linkedador;
import br.com.srs.linkedator.SemanticMicroserviceDescription;

public class LinkedatorTest {

    Linkedador linkedador;

    @Before
    public void configure() throws IOException {

        String ontology = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/domainOntology.owl"), "UTF-8");
        linkedador = new Linkedador(ontology);

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(microservicesDescription);

        String policeReportDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription policeReportDescription = new Gson().fromJson(policeReportDescriptionContent, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(policeReportDescription);

        String vehicleRegistryDescriptionContent = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfVehicleRegistryDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription vehicleRegistryDescription = new Gson().fromJson(vehicleRegistryDescriptionContent, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(vehicleRegistryDescription);

    }

    @Test
    public void mustCreateExplicitLinkInPoliceRepor() throws IOException {
        String policeReport = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/policeReport.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(policeReport);
        System.out.println(linkedRepresentation);
        Assert.assertTrue(linkedRepresentation.contains("\"http://ssp-ontology.com#victim\":\"10.1.1.1/vitima/123456\""));
    }

    @Test
    public void mustCreateInferredLinkInPerson() throws IOException {
        String person = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/person.jsonld"), "UTF-8");
        String linkedRepresentation = linkedador.createLinks(person);
        System.out.println(linkedRepresentation);
        Assert.assertTrue(linkedRepresentation.contains("http://ssp-ontology.com#envolvedIn\":\"10.1.1.2/reports/13579"));
        Assert.assertTrue(linkedRepresentation.contains("http://ssp-ontology.com#ownerOf\":\"10.1.1.3/report/owner/13579"));

    }
}
