package br.com.srs.linkedador;

import java.util.ArrayList;
import java.util.List;

public class SemanticMicroserviceDescription {

    private String uriBase;
    private List<SemanticResource> semanticResources = new ArrayList<>();

    public String getUriBase() {
        return uriBase;
    }

    public void setUriBase(String uriBase) {
        this.uriBase = uriBase;
    }

    public List<SemanticResource> getSemanticResources() {
        return semanticResources;
    }

    public void setSemanticResources(List<SemanticResource> semanticResources) {
        this.semanticResources = semanticResources;
    }

}