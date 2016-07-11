package br.ufsc.inf.lapesd.linkedator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.util.iterator.ExtendedIterator;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;

public class Linkedator {

    private List<SemanticMicroserviceDescription> semanticMicroserviceDescriptions = new ArrayList<>();
    private OntologyReader ontologyReader;

    public Linkedator(String ontology) {
        this.ontologyReader = new OntologyReader(ontology);

    }

    public void registryDescription(SemanticMicroserviceDescription semanticMicroserviceDescription) {
        this.semanticMicroserviceDescriptions.add(semanticMicroserviceDescription);
    }

    public String createLinks(String resourceRepresentation) {
        String linkedResourceRepresentation = resourceRepresentation;
        linkedResourceRepresentation = createExplicitLinks(resourceRepresentation);
        linkedResourceRepresentation = createInferredLinks(linkedResourceRepresentation);

        return linkedResourceRepresentation;
    }

    private String createInferredLinks(String resourceRepresentation) {
        String linkedResourceRepresentation = resourceRepresentation;
        JsonObject jsonObjectResourceRepresentation = new JsonParser().parse(resourceRepresentation).getAsJsonObject();

        List<ObjectProperty> applicableObjectProperties = new ArrayList<>();
        String resourceRepresentationId = JsonPath.read(resourceRepresentation, "$['@type']");

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
                 * in case the range/domain os ah given obj-property have no
                 * implementation
                 */
                if (semanticResources.isEmpty()) {
                    continue;
                }

                for (SemanticResource selectedSemanticResource : semanticResources) {

                    JsonElement jsonElementInferredLink = jsonObjectResourceRepresentation.get(objectProperty.getURI());

                    Set<String> listAllPropertyIds = listAllPropertyIds(resourceRepresentation);
                    UriTemplate uriTemplateMatch = getUriTemplateMatch(selectedSemanticResource, listAllPropertyIds);

                    if (uriTemplateMatch == null) {
                        continue;
                    }

                    JsonObject innerInferredObject = new JsonObject();
                    innerInferredObject.addProperty("@type", selectedSemanticResource.getEntity());
                    innerInferredObject.addProperty("http://www.w3.org/2000/01/rdf-schema#seeAlso", resolveLink(selectedSemanticResource, uriTemplateMatch, resourceRepresentation));

                    if (jsonElementInferredLink == null) {
                        jsonObjectResourceRepresentation.add(objectProperty.getURI(), new Gson().toJsonTree(innerInferredObject));
                    }

                    else if (jsonElementInferredLink.isJsonObject()) {
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

                    linkedResourceRepresentation = jsonObjectResourceRepresentation.toString();

                }

            }
        }

        return linkedResourceRepresentation;
    }

    private String createExplicitLinks(String resourceRepresentation) {
        String linkedResourceRepresentation = resourceRepresentation;
        JsonObject jsonObjectResourceRepresentation = new JsonParser().parse(resourceRepresentation).getAsJsonObject();

        List<ObjectProperty> listObjectProperties = listObjectProperties(resourceRepresentation);
        for (ObjectProperty objectProperty : listObjectProperties) {
            ExtendedIterator<? extends OntResource> listRange = objectProperty.listRange();
            while (listRange.hasNext()) {
                String objectPropertyRange = listRange.next().getURI();
                List<SemanticResource> semanticResources = getSemanticResourceByEntity(objectPropertyRange);

                /*
                 * in case the range/domain os ah given obj-property have no
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
                            asJsonObject.addProperty("http://www.w3.org/2000/01/rdf-schema#seeAlso", resolveLink(selectedSemanticResource, uriTemplateMatch, asJsonObject.toString()));
                        }
                    } else if (jsonElement.isJsonObject()) {
                        JsonObject asJsonObject = jsonElement.getAsJsonObject();
                        Set<String> listAllPropertyIds = listAllPropertyIds(asJsonObject.toString());
                        UriTemplate uriTemplateMatch = getUriTemplateMatch(selectedSemanticResource, listAllPropertyIds);
                        if (uriTemplateMatch == null) {
                            continue;
                        }
                        asJsonObject.addProperty("@type", selectedSemanticResource.getEntity());
                        asJsonObject.addProperty("http://www.w3.org/2000/01/rdf-schema#seeAlso", resolveLink(selectedSemanticResource, uriTemplateMatch, asJsonObject.toString()));
                    }

                    linkedResourceRepresentation = jsonObjectResourceRepresentation.toString();
                }

            }
        }
        return linkedResourceRepresentation;
    }

    private UriTemplate getUriTemplateMatch(SemanticResource semanticResource, Set<String> listOfProperties) {
        UriTemplate optimalUriTemplate = null;

        List<UriTemplate> uriTemplates = semanticResource.getUriTemplates();
        for (UriTemplate uriTemplate : uriTemplates) {
            Map<String, String> parameters = uriTemplate.getParameters();
            Collection<String> values = parameters.values();
            if (listOfProperties.containsAll(values)) {
                optimalUriTemplate = uriTemplate;
                break;
            }
        }
        return optimalUriTemplate;
    }

    private String resolveLink(SemanticResource semanticResource, UriTemplate uriTemplate, String representation) {
        String link = String.format("%s/%s", semanticResource.getSemanticMicroserviceDescription().getUriBase(), uriTemplate.getUri());
        Map<String, String> parameters = uriTemplate.getParameters();
        Set<String> uriTemplateParams = parameters.keySet();
        for (String param : uriTemplateParams) {
            String uriPropertyOfParam = parameters.get(param);
            String paramValuepresentedInResourceRep = JsonPath.read(representation, String.format("$['%s']", uriPropertyOfParam));
            link = link.replace("{" + param + "}", paramValuepresentedInResourceRep);
        }
        return link;
    }

    private List<SemanticResource> getSemanticResourceByEntity(String entity) {
        List<SemanticResource> selectedSemanticResources = new ArrayList<>();
        for (SemanticMicroserviceDescription semanticMicroserviceDescription : semanticMicroserviceDescriptions) {
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

}
