package br.ufsc.inf.lapesd.linkedator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import com.google.gson.Gson;

public class SemanticMicroserviceDescription {

    private String ipAddress;
    private String serverPort;
    private String uriBase;
    private List<SemanticResource> semanticResources = new ArrayList<>();
    private String ontologyBase64;

    public String getMicroserviceFullPath() {
        UriBuilder builder = UriBuilder.fromPath("http://{ipAddress}:{serverPort}").path(uriBase);
        URI uri = builder.build(ipAddress, serverPort);
        return uri.toASCIIString();
    }

    public String getUriBase() {
        return uriBase;
    }

    public void setUriBase(String uriBase) {
        this.uriBase = uriBase;
    }

    public List<SemanticResource> getSemanticResources() {
        /* Workarround */
        for (SemanticResource semanticResource : semanticResources) {
            semanticResource.setSemanticMicroserviceDescription(this);
        }
        return semanticResources;
    }

    public void setSemanticResources(List<SemanticResource> semanticResources) {
        this.semanticResources = semanticResources;
    }

    public String getServerPort() {
        return serverPort;
    }

    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public String getOntologyBase64() {
        return ontologyBase64;
    }

    public void setOntologyBase64(String ontologyBase64) {
        this.ontologyBase64 = ontologyBase64;
    }

}