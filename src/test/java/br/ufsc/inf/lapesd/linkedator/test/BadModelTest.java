package br.ufsc.inf.lapesd.linkedator.test;

import br.ufsc.inf.lapesd.linkedator.LinkCreationException;
import br.ufsc.inf.lapesd.linkedator.Linkedator;
import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class BadModelTest {
    private Linkedator linkedator;

    @Before
    public void configure() throws IOException {
        linkedator = TestUtils.createLinkedator(getClass().getResourceAsStream(
                "/badModel/domainOntology.ttl"), Lang.TURTLE);

        String smdString = IOUtils.toString(this.getClass().getResourceAsStream("/badModel/microserviceOfPeopleDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription smd = new Gson().fromJson(smdString, SemanticMicroserviceDescription.class);
        smd.setIpAddress("192.168.10.1");
        smd.setServerPort("8080");
        smd.setUriBase("/service/");
        linkedator.register(smd);
    }

    @Test
    public void testGoodPerson() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/badModel/good-person.ttl")) {
            RDFDataMgr.read(model, in, Lang.TURTLE);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Resource r = model.createResource("http://10.1.1.1/people-microservice/13579");
        Assert.assertTrue(r.hasProperty(OWL2.sameAs));
        Assert.assertEquals(r.getPropertyResourceValue(OWL2.sameAs),
                model.createResource("http://192.168.10.1:8080/service/person/13579"));
    }

    @Test
    public void testBadPerson() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/badModel/bad-person.ttl")) {
            RDFDataMgr.read(model, in, Lang.TURTLE);
        }
        long old = model.size();

        boolean caught = false;
        try {
            linkedator.createLinks(model, new NullLinkVerifier());
        } catch (LinkCreationException e) {
            caught = true;
        }
        Assert.assertTrue(caught);
        Assert.assertEquals(old, model.size());
    }
}
