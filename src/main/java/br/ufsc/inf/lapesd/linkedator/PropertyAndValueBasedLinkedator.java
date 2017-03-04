package br.ufsc.inf.lapesd.linkedator;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public class PropertyAndValueBasedLinkedator {

    private OntologyReader ontologyReader;
    private Map<String, SemanticMicroserviceDescription> registeredMicroservices = new HashMap<>();

    private boolean cacheEnabled = true;
    private int cacheMaximumSize = 100;
    private int cacheExpireAfterAccessSeconds = 30;
    private Cache<String, Boolean> linkCache = CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(cacheMaximumSize).expireAfterAccess(cacheExpireAfterAccessSeconds, TimeUnit.SECONDS).build();

    public PropertyAndValueBasedLinkedator(String ontology) {
        this.ontologyReader = new OntologyReader(ontology);
    }

    public void enableCache(boolean enable) {
        this.cacheEnabled = enable;
    }

    public void setCacheConfiguration(int maximumSize, int expireAfterAccessSeconds, Cache<String, Boolean> linkCache) {
        this.cacheExpireAfterAccessSeconds = expireAfterAccessSeconds;
        this.cacheMaximumSize = maximumSize;
        this.linkCache = linkCache;
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

    public String createLinks(String resourceRepresentation, boolean verifyLinks) {
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

                                    if ((verifyLinks && !isLinkValid(resolvedLink))) {
                                        continue;
                                    }

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
                    String innerObjectLinked = createLinks(entry.getValue().toString(), verifyLinks);
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

    protected boolean isLinkValid(String link) {
        if (!cacheEnabled) {
            return isLinkValidNoCache(link);
        }

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(link).queryParam("linkedatorOptions", "linkVerify");

        Boolean isCached = linkCache.getIfPresent(link);
        if (isCached != null) {
            System.out.println(String.format("verifying: %s (cached)", link));
            return isCached;
        }

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        try {
            System.out.println(String.format("verifying: %s", link));
            Response response = invocationBuilder.head();

            int status = response.getStatus();
            if (status == 200) {
                linkCache.put(link, true);
                return true;
            }
            linkCache.put(link, false);
            return false;

        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isLinkValidNoCache(String link) {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(link).queryParam("linkedatorOptions", "linkVerify");

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        try {
            System.out.println(String.format("verifying: %s", link));
            Response response = invocationBuilder.head();

            int status = response.getStatus();
            if (status == 200) {
                return true;
            }
            return false;

        } catch (Exception e) {
            return false;
        }
    }

}
