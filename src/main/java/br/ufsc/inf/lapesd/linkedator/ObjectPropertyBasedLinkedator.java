package br.ufsc.inf.lapesd.linkedator;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.util.iterator.ExtendedIterator;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

public class ObjectPropertyBasedLinkedator {

    private Map<String, SemanticMicroserviceDescription> registeredMicroservices = new HashMap<>();
    private OntologyReader ontologyReader;

    private boolean cacheEnabled = true;
    private int cacheMaximumSize = 100;
    private int cacheExpireAfterAccessSeconds = 30;
    private Cache<String, Boolean> linkCache = CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(cacheMaximumSize).expireAfterAccess(cacheExpireAfterAccessSeconds, TimeUnit.SECONDS).build();

    public ObjectPropertyBasedLinkedator(OntologyReader ontologyReader) {
        this.ontologyReader = ontologyReader;
    }

    public void enableCache(boolean enable) {
        this.cacheEnabled = enable;
    }

    public void setLinkCache(Cache<String, Boolean> linkCache) {
        this.linkCache = linkCache;
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
                // System.out.println(microserviceFullPath + " updated");
            } else {
                // System.out.println(microserviceFullPath + " registered");
            }
            registeredMicroservices.put(microserviceFullPath, semanticMicroserviceDescription);

        } catch (Exception e) {
            throw new RuntimeException("Invalid description");
        }
    }

    public String createLinks(String resourceRepresentation, boolean verifyLinks) {
        String linkedResourceRepresentation = resourceRepresentation;

        linkedResourceRepresentation = createExplicitLinks(resourceRepresentation, verifyLinks);
        linkedResourceRepresentation = createInferredLinks(linkedResourceRepresentation, verifyLinks);

        return linkedResourceRepresentation;
    }

    private String createInferredLinks(String resourceRepresentation, boolean verifyLinks) {
        JsonElement parseRepresentation = new JsonParser().parse(resourceRepresentation);
        if (!parseRepresentation.isJsonObject()) {
            JsonArray jsonArrayWithLinks = new JsonArray();
            JsonArray asJsonArray = parseRepresentation.getAsJsonArray();
            for (int i = 0; i < asJsonArray.size(); i++) {
                JsonObject createLinks = createInferredLinks(asJsonArray.get(i).getAsJsonObject(), verifyLinks);
                jsonArrayWithLinks.add(createLinks);
            }
            return jsonArrayWithLinks.toString();
        }

        if (parseRepresentation.isJsonObject()) {
            return createInferredLinks(parseRepresentation.getAsJsonObject(), verifyLinks).toString();
        }

        return resourceRepresentation;
    }

    private String createExplicitLinks(String resourceRepresentation, boolean verifyLinks) {
        String linkedResourceRepresentation = resourceRepresentation;
        JsonElement parseRepresentation = new JsonParser().parse(resourceRepresentation);

        if (parseRepresentation.isJsonObject()) {
            return createExplicitLinks(parseRepresentation.getAsJsonObject(), verifyLinks).toString();
        }

        if (parseRepresentation.isJsonArray()) {
            JsonArray jsonArrayWithLinks = new JsonArray();
            JsonArray asJsonArray = parseRepresentation.getAsJsonArray();
            for (int i = 0; i < asJsonArray.size(); i++) {
                JsonObject createLinks = createExplicitLinks(asJsonArray.get(i).getAsJsonObject(), verifyLinks);
                jsonArrayWithLinks.add(createLinks);
            }

            return jsonArrayWithLinks.toString();
        }
        return linkedResourceRepresentation;
    }

    private JsonObject createInferredLinks(JsonObject jsonObjectResourceRepresentation, boolean verifyLinks) {
        List<ObjectProperty> applicableObjectProperties = new ArrayList<>();
        String resourceRepresentationId = JsonPath.read(jsonObjectResourceRepresentation.toString(), "$['@type']");

        /*
         * finding all applicable obj-properties that have the representation id
         * as domain
         */
        List<ObjectProperty> objectProperties = this.ontologyReader.getObjectProperties();
        for (ObjectProperty objectProperty : objectProperties) {
            if (objectProperty.getDomain().getURI().equalsIgnoreCase(resourceRepresentationId)) {
                applicableObjectProperties.add(objectProperty);
            }
        }

        for (ObjectProperty objectProperty : applicableObjectProperties) {
            ExtendedIterator<? extends OntResource> listRange = objectProperty.listRange();
            while (listRange.hasNext()) {
                String objectPropertyRange = listRange.next().getURI();
                List<SemanticResource> semanticResources = getSemanticResourceByEntity(objectPropertyRange);
                /*
                 * in case the range/domain of an given obj-property is not
                 * present
                 */
                if (semanticResources.isEmpty()) {
                    continue;
                }

                for (SemanticResource selectedSemanticResource : semanticResources) {

                    JsonElement jsonElementInferredLink = jsonObjectResourceRepresentation.get(objectProperty.getURI());

                    Set<String> listAllPropertyIds = listAllPropertyIds(jsonObjectResourceRepresentation.toString());
                    UriTemplate uriTemplateMatch = getUriTemplateMatch(selectedSemanticResource, listAllPropertyIds);

                    if (uriTemplateMatch == null) {
                        continue;
                    }

                    JsonObject innerInferredObject = new JsonObject();
                    String resolvedLink = resolveLink(selectedSemanticResource, uriTemplateMatch, jsonObjectResourceRepresentation.toString());

                    if (!verifyLinks || (verifyLinks && isLinkValid(resolvedLink))) {
                        innerInferredObject.addProperty("@type", selectedSemanticResource.getEntity());
                        innerInferredObject.addProperty("http://www.w3.org/2002/07/owl#sameAs", resolvedLink);
                    }

                    if (jsonElementInferredLink == null || jsonElementInferredLink.isJsonNull()) {
                        if (innerInferredObject.entrySet().size() > 0) {
                            jsonObjectResourceRepresentation.add(objectProperty.getURI(), new Gson().toJsonTree(innerInferredObject));
                        }
                    } else {

                        if (jsonElementInferredLink.isJsonObject()) {
                            JsonArray array = new JsonArray();
                            array.add(new Gson().toJsonTree(jsonElementInferredLink));
                            array.add(new Gson().toJsonTree(innerInferredObject));
                            jsonObjectResourceRepresentation.remove(objectProperty.getURI());
                            jsonObjectResourceRepresentation.add(objectProperty.getURI(), new Gson().toJsonTree(array));
                        }

                        else if (jsonElementInferredLink.isJsonArray()) {
                            jsonElementInferredLink.getAsJsonArray().add(new Gson().toJsonTree(innerInferredObject));
                            jsonObjectResourceRepresentation.remove(objectProperty.getURI());
                            jsonObjectResourceRepresentation.add(objectProperty.getURI(), new Gson().toJsonTree(jsonElementInferredLink));
                        }
                    }
                }
            }
        }

        return jsonObjectResourceRepresentation;
    }

    private JsonObject createExplicitLinks(JsonObject jsonObjectResourceRepresentation, boolean verifyLinks) {
        List<ObjectProperty> listObjectProperties = listObjectProperties(jsonObjectResourceRepresentation.toString());
        for (ObjectProperty objectProperty : listObjectProperties) {
            ExtendedIterator<? extends OntResource> listRange = objectProperty.listRange();
            while (listRange.hasNext()) {
                String objectPropertyRange = listRange.next().getURI();
                List<SemanticResource> semanticResources = getSemanticResourceByEntity(objectPropertyRange);

                /*
                 * in case the range/domain of a given obj-property have no
                 * implementation
                 */
                if (semanticResources.isEmpty()) {
                    continue;
                }

                for (SemanticResource selectedSemanticResource : semanticResources) {

                    boolean isRangeObjectPropertyEqualsEntity = objectPropertyRange.equalsIgnoreCase(selectedSemanticResource.getEntity());
                    if (!isRangeObjectPropertyEqualsEntity) {
                        continue;
                    }

                    JsonElement jsonElement = jsonObjectResourceRepresentation.get(objectProperty.getURI());
                    if (jsonElement.isJsonArray()) {
                        JsonArray asJsonArray = jsonElement.getAsJsonArray();
                        Iterator<JsonElement> iterator = asJsonArray.iterator();
                        while (iterator.hasNext()) {
                            JsonElement next = iterator.next();
                            JsonObject asJsonObject = next.getAsJsonObject();
                            Set<String> listAllPropertyIds = listAllPropertyIds(asJsonObject.toString());
                            UriTemplate uriTemplateMatch = getUriTemplateMatch(selectedSemanticResource, listAllPropertyIds);
                            if (uriTemplateMatch == null) {
                                continue;
                            }
                            asJsonObject.addProperty("@type", selectedSemanticResource.getEntity());
                            String resolvedLink = resolveLink(selectedSemanticResource, uriTemplateMatch, asJsonObject.toString());
                            if (!verifyLinks || (verifyLinks && isLinkValid(resolvedLink))) {
                                asJsonObject.addProperty("http://www.w3.org/2002/07/owl#sameAs", resolvedLink);
                            }
                        }
                    } else if (jsonElement.isJsonObject()) {
                        JsonObject asJsonObject = jsonElement.getAsJsonObject();
                        Set<String> listAllPropertyIds = listAllPropertyIds(asJsonObject.toString());
                        UriTemplate uriTemplateMatch = getUriTemplateMatch(selectedSemanticResource, listAllPropertyIds);
                        if (uriTemplateMatch == null) {
                            continue;
                        }
                        asJsonObject.addProperty("@type", selectedSemanticResource.getEntity());
                        String resolvedLink = resolveLink(selectedSemanticResource, uriTemplateMatch, asJsonObject.toString());

                        if (!verifyLinks || (verifyLinks && isLinkValid(resolvedLink))) {
                            asJsonObject.addProperty("http://www.w3.org/2002/07/owl#sameAs", resolvedLink);
                        }
                    }
                }
            }
        }
        return jsonObjectResourceRepresentation;
    }

    private UriTemplate getUriTemplateMatch(SemanticResource semanticResource, Set<String> listOfProperties) {
        UriTemplate optimalUriTemplate = null;

        Set<String> equivalentProperties = loadEquivalentProperties(listOfProperties);

        List<UriTemplate> uriTemplates = semanticResource.getUriTemplates();
        for (UriTemplate uriTemplate : uriTemplates) {
            Map<String, String> parameters = uriTemplate.getParameters();
            Collection<String> values = parameters.values();
            if (equivalentProperties.containsAll(values)) {
                optimalUriTemplate = uriTemplate;
                break;
            }
        }
        return optimalUriTemplate;
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

    private String resolveLink(SemanticResource semanticResource, UriTemplate uriTemplate, String representation) {
        UriBuilder builder = UriBuilder.fromPath(semanticResource.getSemanticMicroserviceDescription().getMicroserviceFullPath()).path(uriTemplate.getUri());

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

    private List<SemanticResource> getSemanticResourceByEntity(String entity) {
        List<SemanticResource> selectedSemanticResources = new ArrayList<>();
        for (SemanticMicroserviceDescription semanticMicroserviceDescription : registeredMicroservices.values()) {
            List<SemanticResource> semanticResources = semanticMicroserviceDescription.getSemanticResources();
            for (SemanticResource semanticResource : semanticResources) {
                if (semanticResource.getEntity().equalsIgnoreCase(entity)) {
                    selectedSemanticResources.add(semanticResource);
                }
            }
        }

        return selectedSemanticResources;
    }

    private List<ObjectProperty> listObjectProperties(String resourceRepresentation) {
        List<ObjectProperty> objectProperties = new ArrayList<>();
        Map<String, ?> obj = JsonPath.read(resourceRepresentation, "$");
        Set<String> uriProperty = obj.keySet();
        for (String uri : uriProperty) {
            ObjectProperty objectProperty = ontologyReader.getMapUriObjectProperty().get(uri);
            if (objectProperty != null) {
                objectProperties.add(objectProperty);
            }
        }

        return objectProperties;
    }

    private Set<String> listAllPropertyIds(String resourceRepresentation) {
        Map<String, ?> obj = JsonPath.read(resourceRepresentation, "$");
        return obj.keySet();
    }

    protected boolean isLinkValid(String link) {
        if (!cacheEnabled) {
            return isLinkValidNoCache(link);
        }

        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(link).queryParam("linkedatorOptions", "linkVerify");

        Boolean isCached = linkCache.getIfPresent(link);
        if (isCached != null) {
            return isCached;
        }

        Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);

        try {
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
