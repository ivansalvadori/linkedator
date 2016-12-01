package br.ufsc.inf.lapesd.linkedator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.iterator.ExtendedIterator;

public class OntologyReader { 
	
	
	private Map<String, ObjectProperty> mapUriObjectProperty = new HashMap<>();
	private List<ObjectProperty> objectProperties = new ArrayList<>();
	private OntModel ontoModel; 

    public OntologyReader(String ontology) {
        ontoModel = this.loadOntology(ontology);
        this.objectProperties = ontoModel.listObjectProperties().toList();
        for (ObjectProperty objectProperty : objectProperties) {
            mapUriObjectProperty.put(objectProperty.getURI(), objectProperty);
        }
    }
    
    public List<ObjectProperty> getObjectProperties() {
        return objectProperties;
    }
    
    public Map<String, ObjectProperty> getMapUriObjectProperty() {
        return mapUriObjectProperty;
    }
    
    
	private OntModel loadOntology(String ontology) {
		OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
		try {
			InputStream in = new ByteArrayInputStream(ontology.getBytes(StandardCharsets.UTF_8));

			try {
				ontoModel.read(in, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (JenaException je) {
			System.err.println("ERROR" + je.getMessage());
			je.printStackTrace();
		}

		return ontoModel;
	}

    public Set<String> getEquivalentProperties(String property) {
        Set<String> eqvProperties = new HashSet<>();
        OntProperty ontProperty = this.ontoModel.getOntProperty(property);
        if(ontProperty == null){
            return null;
        }
        ExtendedIterator<? extends OntProperty> listEquivalentProperties = ontProperty.listEquivalentProperties();
        while(listEquivalentProperties.hasNext()){
            OntProperty equivalentProperty = listEquivalentProperties.next();
            eqvProperties.add(equivalentProperty.getURI());
        }
        
        if(eqvProperties.isEmpty()){
            return null;
        }
        return eqvProperties;
    }
	
	

}
