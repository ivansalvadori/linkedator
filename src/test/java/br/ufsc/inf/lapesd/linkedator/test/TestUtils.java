package br.ufsc.inf.lapesd.linkedator.test;

import br.ufsc.inf.lapesd.linkedator.ModelBasedLinkedator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import javax.annotation.Nonnull;
import javax.annotation.WillClose;
import java.io.IOException;
import java.io.InputStream;


public class TestUtils {
    public static final String SPARQL_PROLOGUE = "PREFIX ssp: <http://ssp-ontology.com#>\n" +
            "PREFIX pol: <http://police-ontology.com#>\n" +
            "PREFIX per: <http://person-ontology#>\n" +
            "PREFIX rdf: <" + RDF.getURI() + ">\n" +
            "PREFIX rdfs: <" + RDFS.getURI() + ">\n" +
            "PREFIX owl: <" + OWL2.getURI() + ">\n" +
            "PREFIX sch: <http://schema.org/>\n" +
            "PREFIX city: <http://city.example.org/ns#>\n" +
            "PREFIX state: <http://state.example.org/ns#>\n";

    public static ModelBasedLinkedator createLinkedator(@WillClose @Nonnull InputStream in,
                                                        @Nonnull Lang lang) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, in, lang);
        in.close();
        ModelBasedLinkedator linkedator = new ModelBasedLinkedator();
        linkedator.updateOntologies(model);
        return linkedator;
    }

}