package br.ufsc.inf.lapesd.linkedator.test;

import br.ufsc.inf.lapesd.linkedator.LinkCreationException;
import br.ufsc.inf.lapesd.linkedator.Linkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryExecutionFactory;
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

public class CitiesTest {
    private Linkedator linkedator;

    @Before
    public void configure() throws IOException {
        linkedator = TestUtils.createLinkedator(getClass().getResourceAsStream(
                "/cities/domainOntology.ttl"), Lang.TURTLE);

        String smdString = IOUtils.toString(this.getClass().getResourceAsStream("/cities/microserviceOfCities.json"), "UTF-8");
        SemanticMicroserviceDescription smd = new Gson().fromJson(smdString, SemanticMicroserviceDescription.class);
        smd.setIpAddress("192.168.10.1");
        linkedator.register(smd);

        smdString = IOUtils.toString(this.getClass().getResourceAsStream("/cities/microserviceOfStates.json"), "UTF-8");
        smd = new Gson().fromJson(smdString, SemanticMicroserviceDescription.class);
        smd.setIpAddress("192.168.10.2");
        linkedator.register(smd);
    }

    @Test
    public void test() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/cities/city.ttl")) {
            RDFDataMgr.read(model, in, Lang.TURTLE);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://192.168.10.1/city/0> city:inState ?x.\n" +
                "  ?x a state:State; owl:sameAs <http://192.168.10.2/state/RO>.\n" +
                "}", model).execAsk());
    }
}
