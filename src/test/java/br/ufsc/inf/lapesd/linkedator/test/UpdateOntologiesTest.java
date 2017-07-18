package br.ufsc.inf.lapesd.linkedator.test;

import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class UpdateOntologiesTest {

    private ModelBasedLinkedator linkedator;

    @Before
    public void configure() throws IOException {
        linkedator = new ModelBasedLinkedator();
        try (InputStream in = this.getClass().getResourceAsStream("/updateOntologiesTest/domainOntology.ttl")) {
            RDFDataMgr.read(linkedator.getOntologies(), in, Lang.TURTLE);
        }

        String smdString = IOUtils.toString(this.getClass().getResourceAsStream("/updateOntologiesTest/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription smd = new Gson().fromJson(smdString, SemanticMicroserviceDescription.class);
        smd.setIpAddress("192.168.10.1");
        smd.setServerPort("8080");
        smd.setUriBase("/service/");
        linkedator.register(smd);

    }

    @Test
    public void test() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/updateOntologiesTest/person-ssp.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        long old = model.size();
        linkedator.createLinks(model, new NullLinkVerifier());
        Assert.assertEquals(old, model.size());

        Model updated = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/updateOntologiesTest/domainOntology-2.ttl")) {
            RDFDataMgr.read(updated, in, Lang.TURTLE);
        }
        linkedator.updateOntologies(updated);
        InfModel infModel = ModelFactory.createInfModel(linkedator.getReasoner().bind(model.getGraph()));
        Resource r = infModel.createResource("http://10.1.1.1/people-microservice/13579");


        linkedator.createLinks(model, new NullLinkVerifier());
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE{\n" +
                "  <http://10.1.1.1/people-microservice/13579> owl:sameAs <http://192.168.10.1:8080/service/person/13579>.\n" +
                "}", model).execAsk());
    }

}
