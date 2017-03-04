package br.ufsc.inf.lapesd.linkedator;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public class PropertyAndValueLinkedator {

    private OntologyReader ontologyReader;
    private Map<String, SemanticMicroserviceDescription> registeredMicroservices = new HashMap<>();

    public PropertyAndValueLinkedator(String ontology) {
        this.ontologyReader = new OntologyReader(ontology);
    }

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
                if (entry.getValue().isJsonPrimitive()) {
                    for (SemanticMicroserviceDescription semanticMicroserviceDescription : registeredMicroservices.values()) {
                        List<SemanticResource> semanticResources = semanticMicroserviceDescription.getSemanticResources();
                        for (SemanticResource semanticResource : semanticResources) {
                            String resourceRepresentationId = null;
                            try {
                                resourceRepresentationId = JsonPath.read(resourceRepresentation, "$['@type']");
                            } catch (PathNotFoundException e) {
                                resourceRepresentationId = null;
                            }
                            if (semanticResource.getEntity().equals(resourceRepresentationId)) {
                                continue;
                            }

                            Set<String> listAllPropertyIds = listAllPropertyIds(resourceRepresentation);
                            Set<String> equivalentProperties = loadEquivalentProperties(listAllPropertyIds);

                            List<UriTemplate> uriTemplates = semanticResource.getUriTemplates();
                            // uriTemplates.addAll(searchForMoreUriTemplates(equivalentProperties));

                            for (UriTemplate uriTemplate : uriTemplates) {
                                Map<String, String> parameters = uriTemplate.getParameters();
                                Collection<String> templateProperties = parameters.values();

                                boolean satisfied = false;
                                if (equivalentProperties.containsAll(templateProperties)) {
                                    satisfied = true;
                                } else {
                                    satisfied = false;
                                }

                                if (satisfied) {
                                    String resolvedLink = resolveLink(semanticMicroserviceDescription.getMicroserviceFullPath(), uriTemplate, resourceRepresentation);
                                    System.out.println(resolvedLink);
                                    if (linkedResourceRepresentation.contains(resolvedLink)) {
                                        continue;
                                    }
                                    JsonElement element = new JsonParser().parse(linkedResourceRepresentation);
                                    JsonObject representationJsonObject = element.getAsJsonObject();
                                    if (representationJsonObject.get("http://www.w3.org/2000/01/rdf-schema#seeAlso") == null) {
                                        element.getAsJsonObject().addProperty("http://www.w3.org/2000/01/rdf-schema#seeAlso", resolvedLink);
                                    } else {
                                        if (!representationJsonObject.get("http://www.w3.org/2000/01/rdf-schema#seeAlso").isJsonArray()) {
                                            JsonArray array = new JsonArray();
                                            array.add(new JsonPrimitive(representationJsonObject.get("http://www.w3.org/2000/01/rdf-schema#seeAlso").getAsString()));
                                            array.add(new JsonPrimitive(resolvedLink));
                                            representationJsonObject.remove("http://www.w3.org/2000/01/rdf-schema#seeAlso");
                                            representationJsonObject.add("http://www.w3.org/2000/01/rdf-schema#seeAlso", new Gson().toJsonTree(array));
                                        } else if (representationJsonObject.get("http://www.w3.org/2000/01/rdf-schema#seeAlso").isJsonArray()) {
                                            representationJsonObject.get("http://www.w3.org/2000/01/rdf-schema#seeAlso").getAsJsonArray().add(new JsonPrimitive(resolvedLink));
                                        }

                                    }

                                    linkedResourceRepresentation = element.toString();
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
            Set<String> equivalentProperties = ontologyReader.getEquivalentProperties(uriPropertyOfParam);
            if (equivalentProperties == null) {
                String paramValuepresentedInResourceRep = JsonPath.read(representation, String.format("$['%s']", uriPropertyOfParam));
                resolvedParameters.put(param, paramValuepresentedInResourceRep);
            }

            else {
                equivalentProperties.add(uriPropertyOfParam);
                for (String property : equivalentProperties) {
                    try {
                        String paramValuepresentedInResourceRep = JsonPath.read(representation, String.format("$['%s']", property));
                        resolvedParameters.put(param, paramValuepresentedInResourceRep);
                    } catch (PathNotFoundException e) {
                        continue;
                    }
                }
            }
        }
        builder.resolveTemplates(resolvedParameters);
        URI uri = builder.build();
        String link = uri.toASCIIString();
        return link;
    }

    private Set<String> listAllPropertyIds(String resourceRepresentation) {
        Map<String, ?> obj = JsonPath.read(resourceRepresentation, "$");
        return obj.keySet();
    }

    private Set<String> loadEquivalentProperties(Set<String> listOfProperties) {
        Set<String> eqvProperties = new HashSet<>(listOfProperties);
        for (String property : listOfProperties) {
            Set<String> equivalentProperties = this.ontologyReader.getEquivalentProperties(property);
            if (equivalentProperties != null) {
                eqvProperties.addAll(equivalentProperties);
            }
        }

        return eqvProperties;
    }

}
