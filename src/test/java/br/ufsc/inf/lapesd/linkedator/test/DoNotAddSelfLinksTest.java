package br.ufsc.inf.lapesd.linkedator.test;

import br.ufsc.inf.lapesd.linkedator.Linkedator;
import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
import com.google.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class DoNotAddSelfLinksTest {

    Linkedator linkedator;

    @Before
    public void configure() throws IOException {
        linkedator = TestUtils.createLinkedator(getClass().getResourceAsStream(
                "/doNotAddSelfLinks/domainOntology.owl"), Lang.RDFXML);

        String microserviceOfPoliceReportDescription = IOUtils.toString(this.getClass().getResourceAsStream("/scenario0/microserviceOfPoliceReportDescription.jsonld"), "UTF-8");
        SemanticMicroserviceDescription microservicesDescription = new Gson().fromJson(microserviceOfPoliceReportDescription, SemanticMicroserviceDescription.class);
        microservicesDescription.setIpAddress("10.1.1.2");
        microservicesDescription.setServerPort("80");
        microservicesDescription.setUriBase("/policeReport-microservice/");
        linkedator.register(microservicesDescription);

        SemanticMicroserviceDescription otherDescription = new Gson().fromJson(microserviceOfPoliceReportDescription, SemanticMicroserviceDescription.class);
        otherDescription.setIpAddress("10.1.1.3");
        otherDescription.setServerPort("80");
        otherDescription.setUriBase("/policeReport-microservice/");
        linkedator.register(otherDescription);
    }
    @Test
    public void noSelfLinkPerson() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = getClass().getResourceAsStream("/doNotAddSelfLinks/policeReport.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/report/123> owl:sameAs <http://10.1.1.3/policeReport-microservice/report/123>.\n" +
                "}", model).execAsk());
        Assert.assertFalse(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/report/123> owl:sameAs <http://10.1.1.2/policeReport-microservice/report/123>.\n" +
                "}", model).execAsk());
        Assert.assertFalse(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/report/123> rdfs:seeAlso <http://10.1.1.2/policeReport-microservice/report/123>.\n" +
                "}", model).execAsk());

    }
}
