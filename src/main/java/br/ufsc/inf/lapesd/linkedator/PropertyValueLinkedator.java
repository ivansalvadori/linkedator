package br.ufsc.inf.lapesd.linkedator;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;

public class PropertyValueLinkedator {

    private Map<String, SemanticMicroserviceDescription> registeredMicroservices = new HashMap<>();

    public void registryDescription(SemanticMicroserviceDescription semanticMicroserviceDescription) {
        try {
            String microserviceFullPath = semanticMicroserviceDescription.getMicroserviceFullPath();
            if (registeredMicroservices.get(microserviceFullPath) != null) {
                System.out.println(microserviceFullPath + " updated");
            } else {
                System.out.println(microserviceFullPath + " registered");
            }
            registeredMicroservices.put(microserviceFullPath, semanticMicroserviceDescription);

        } catch (Exception e) {
            throw new RuntimeException("Invalid description");
        }
    }

    public String createLinks(String resourceRepresentation) {
        String linkedResourceRepresentation = resourceRepresentation;

        JsonElement parseRepresentation = new JsonParser().parse(resourceRepresentation);

        if (parseRepresentation.isJsonPrimitive()) {
            System.out.println("primitive");
        }

        if (parseRepresentation.isJsonObject()) {
            Set<Entry<String, JsonElement>> entrySet = parseRepresentation.getAsJsonObject().entrySet();
            for (Entry<String, JsonElement> entry : entrySet) {
                String key = entry.getKey();
                if (entry.getValue().isJsonPrimitive()) {
                    for (SemanticMicroserviceDescription semanticMicroserviceDescription : registeredMicroservices.values()) {
                        List<SemanticResource> semanticResources = semanticMicroserviceDescription.getSemanticResources();
                        for (SemanticResource semanticResource : semanticResources) {
                            List<UriTemplate> uriTemplates = semanticResource.getUriTemplates();
                            for (UriTemplate uriTemplate : uriTemplates) {
                                Map<String, String> parameters = uriTemplate.getParameters();
                                Collection<String> semanticTerms = parameters.values();
                                boolean satisfied = false;
                                for (String term : semanticTerms) {
                                    if (term.equalsIgnoreCase(key)) {
                                        satisfied = true;
                                    } else {
                                        satisfied = false;
                                    }
                                }
                                if (satisfied) {
                                    String resolvedLink = resolveLink(semanticMicroserviceDescription.getMicroserviceFullPath(), uriTemplate, resourceRepresentation);
                                    System.out.println(resolvedLink);
                                    JsonElement element = new JsonParser().parse(linkedResourceRepresentation);
                                    element.getAsJsonObject().addProperty("http://www.w3.org/2002/07/owl#sameAs", resolvedLink);
                                    linkedResourceRepresentation = element.toString();
                                    System.out.println(linkedResourceRepresentation);
                                }
                            }
                        }
                    }
                }
                if (entry.getValue().isJsonObject()) {
                    String innerObjectLinked = createLinks(entry.getValue().toString());
                    JsonElement innerElement = new JsonParser().parse(innerObjectLinked);
                    JsonElement element = new JsonParser().parse(linkedResourceRepresentation);
                    element.getAsJsonObject().add(entry.getKey(), innerElement);
                    linkedResourceRepresentation = element.toString();
                }
                // TODO implement array
            }
        }

        if (parseRepresentation.isJsonArray()) {
            System.out.println("array");

        }

        return linkedResourceRepresentation;
    }
    
    private String resolveLink(String microserviceFullpath, UriTemplate uriTemplate, String representation) {
        UriBuilder builder = UriBuilder.fromPath(microserviceFullpath).path(uriTemplate.getUri());

        Map<String, Object> resolvedParameters = new HashMap<>();
        Map<String, String> parameters = uriTemplate.getParameters();
        Set<String> uriTemplateParams = parameters.keySet();
        for (String param : uriTemplateParams) {
            String uriPropertyOfParam = parameters.get(param);
            String paramValuepresentedInResourceRep = JsonPath.read(representation, String.format("$['%s']", uriPropertyOfParam));
            resolvedParameters.put(param, paramValuepresentedInResourceRep);
        }
        builder.resolveTemplates(resolvedParameters);
        URI uri = builder.build();
        String link = uri.toASCIIString();
        return link;
    }

}
