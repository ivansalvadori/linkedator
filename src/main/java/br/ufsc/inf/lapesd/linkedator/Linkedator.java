package br.ufsc.inf.lapesd.linkedator;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Linkedator {

    private boolean cacheEnabled = true;
    private int cacheMaximumSize = 100;
    private int cacheExpireAfterAccessSeconds = 30;
    private Cache<String, Boolean> linkCache = CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(cacheMaximumSize).expireAfterAccess(cacheExpireAfterAccessSeconds, TimeUnit.SECONDS).build();

    private ObjectPropertyBasedLinkedator objectPropertyBasedLinkedator = null;
    private PropertyAndValueBasedLinkedator propertyAndValueLinkedator = null;

    public Linkedator(String ontology) {
        this.objectPropertyBasedLinkedator = new ObjectPropertyBasedLinkedator(ontology);
        this.objectPropertyBasedLinkedator.setCacheConfiguration(cacheMaximumSize, cacheExpireAfterAccessSeconds, linkCache);
        this.objectPropertyBasedLinkedator.enableCache(cacheEnabled);

        this.propertyAndValueLinkedator = new PropertyAndValueBasedLinkedator(ontology);
        this.propertyAndValueLinkedator.setCacheConfiguration(cacheMaximumSize, cacheExpireAfterAccessSeconds, linkCache);
        this.propertyAndValueLinkedator.enableCache(cacheEnabled);
    }

    public void registryMicroserviceDescription(SemanticMicroserviceDescription semanticMicroserviceDescription) {
        this.objectPropertyBasedLinkedator.registryDescription(semanticMicroserviceDescription);
        this.propertyAndValueLinkedator.registryDescription(semanticMicroserviceDescription);
    }

    public String createLinks(String resourceRepresentation, boolean verifyLinks) {

        Stopwatch time = Stopwatch.createStarted();

        String representationWithLinks = objectPropertyBasedLinkedator.createLinks(resourceRepresentation, verifyLinks);
        representationWithLinks = propertyAndValueLinkedator.createLinks(representationWithLinks, verifyLinks);

        long elapsed = time.elapsed(TimeUnit.MICROSECONDS);
        double processingTime = elapsed / 1000.0;

        String representationWithLinksAndTimeStamp = applyTimeStamp(representationWithLinks, processingTime);

        return representationWithLinksAndTimeStamp;
    }

    private String applyTimeStamp(String representationWithLinks, double processingTime) {
        JsonElement parseRepresentation = new JsonParser().parse(representationWithLinks);
        if (parseRepresentation.isJsonObject()) {
            parseRepresentation.getAsJsonObject().addProperty("linkedatorTime", processingTime);
        } else if (parseRepresentation.isJsonArray()) {
            JsonObject timeObject = new JsonObject();
            timeObject.addProperty("linkedatorTime", processingTime);
            parseRepresentation.getAsJsonArray().add(timeObject);
        }
        return parseRepresentation.toString();
    }

    public void setCacheConfiguration(int maximumSize, int expireAfterAccessSeconds) {
        this.cacheExpireAfterAccessSeconds = expireAfterAccessSeconds;
        this.cacheMaximumSize = maximumSize;
    }

    public void enableCache(boolean enable) {
        this.cacheEnabled = enable;
    }

}
