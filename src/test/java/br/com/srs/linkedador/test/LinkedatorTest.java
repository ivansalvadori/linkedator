package br.com.srs.linkedador.test;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.google.gson.Gson;

import br.com.srs.linkedador.Linkedador;
import br.com.srs.linkedador.SemanticMicroserviceDescription;

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

    }

}
