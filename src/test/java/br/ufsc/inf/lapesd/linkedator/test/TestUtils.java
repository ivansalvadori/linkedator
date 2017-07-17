package br.ufsc.inf.lapesd.linkedator.test;

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;


public class TestUtils {
    public static final String SPARQL_PROLOGUE = "PREFIX ssp: <http://ssp-ontology.com#>\n" +
            "PREFIX pol: <http://police-ontology.com#>\n" +
            "PREFIX per: <http://person-ontology#>\n" +
            "PREFIX rdf: <" + RDF.getURI() + ">\n" +
            "PREFIX rdfs: <" + RDFS.getURI() + ">\n" +
            "PREFIX owl: <" + OWL2.getURI() + ">\n" +
            "PREFIX sch: <http://schema.org/>\n";

}