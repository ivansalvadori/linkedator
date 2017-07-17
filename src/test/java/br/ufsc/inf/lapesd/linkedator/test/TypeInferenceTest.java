package br.ufsc.inf.lapesd.linkedator.test;

import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
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

public class TypeInferenceTest {
    private ModelBasedLinkedator linkedator;

    @Before
    public void configure() throws IOException {
        linkedator = new ModelBasedLinkedator();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario0/domainOntology.owl")) {
            RDFDataMgr.read(linkedator.getOntologies(), in, Lang.RDFXML);
        }
    }

    @Test
    public void testInferType() throws IOException {
        Model model = ModelFactory.createDefaultModel();
        try (InputStream in = this.getClass().getResourceAsStream("/scenario0/policeReport.jsonld")) {
            RDFDataMgr.read(model, in, Lang.JSONLD);
        }
        linkedator.createLinks(model, new NullLinkVerifier());

        Assert.assertFalse(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {?x owl:sameAs ?y.}", model).execAsk());
        Assert.assertFalse(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {?x rdfs:seeAlso ?y.}", model).execAsk());
        /* type assertion should still be added */
        Assert.assertTrue(QueryExecutionFactory.create(TestUtils.SPARQL_PROLOGUE +
                "ASK WHERE {\n" +
                "  <http://10.1.1.2/policeReport-microservice/123> ssp:victim/rdf:type sch:Person.\n" +
                "}", model).execAsk());

    }
}
