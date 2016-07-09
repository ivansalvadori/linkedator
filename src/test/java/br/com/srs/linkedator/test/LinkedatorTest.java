package br.com.srs.linkedator.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

import br.com.srs.linkedator.Linkedador;
import br.com.srs.linkedator.SemanticMicroserviceDescription;

public class LinkedatorTest {

    @Test
    public void mustCreateLinkInPoliceRepor() throws IOException {

        String ontology = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/domainOntology.owl"), "UTF-8");

        Linkedador linkedador = new Linkedador(ontology);
        String policeReport = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/policeReport.jsonld"), "UTF-8");

        String microserviceOfPeopleDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario1/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPeopleDescription, SemanticMicroserviceDescription.class);
        linkedador.registryDescription(microservicesDescription);

        String linkedRepresentation = linkedador.createLinks(policeReport);
        System.out.println(linkedRepresentation);
        Assert.assertTrue(linkedRepresentation.contains("\"http://ssp-ontology.com#victim\":\"10.1.1.1/vitima/123456\""));
    }

}
