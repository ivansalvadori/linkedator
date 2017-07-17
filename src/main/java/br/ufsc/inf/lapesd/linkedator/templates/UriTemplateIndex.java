package br.ufsc.inf.lapesd.linkedator.templates;

import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;
import com.google.common.base.Preconditions;
import org.apache.jena.rdf.model.*;

import javax.annotation.Nonnull;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.*;
import java.util.stream.Stream;

import static org.apache.jena.rdf.model.ResourceFactory.*;

/**
 * This stores {@link SemanticMicroserviceDescription} objects and allows some queries
 * on their URI templates.
 */
public class UriTemplateIndex {
    LinkedHashSet<SemanticMicroserviceDescription> descriptions = new LinkedHashSet<>();

    @Nonnull
    public UriTemplateIndex register(SemanticMicroserviceDescription smd) {
        descriptions.add(smd);
        return this;
    }

    public Streamer streamAny() {
        return new Streamer(descriptions.stream().flatMap(smd -> smd.getSemanticResources().stream()
                .flatMap(sr -> sr.getUriTemplates().stream()
                        .map(ut -> new ExtendedUriTemplate(ut, sr.getEntity(), smd)))));
    }

    public Streamer streamWithType(Resource type) {
        if (!type.isURIResource())
            return new Streamer(Stream.empty());
        return new Streamer(descriptions.stream().flatMap(smd -> smd.getSemanticResources().stream()
                .filter(sr -> sr.getEntity().equals(type.getURI()))
                .flatMap(sr -> sr.getUriTemplates().stream()
                        .map(ut -> new ExtendedUriTemplate(ut, sr.getEntity(), smd)))));
    }

    public class Streamer {
        private Stream<ExtendedUriTemplate> stream;

        private Streamer(Stream<ExtendedUriTemplate> stream) {
            this.stream = stream;
        }

        public Stream<UriTemplateMatch> usingShallowPropertiesOf(Resource resource) {
            Preconditions.checkArgument(resource.getModel() != null);
            return stream.map(ut -> {
                String base = ut.getParent().getMicroserviceFullPath();
                UriBuilder builder = UriBuilder.fromUri(base).path(ut.getUri());
                Map<String, String> values = new HashMap<>();
                for (Map.Entry<String, String> entry : ut.getParameters().entrySet()) {
                    Property property = createProperty(entry.getValue());
                    Statement s = resource.getProperty(property);
                    if (s == null)
                        return null;
                    String string = toString(s.getObject());
                    if (string == null)
                        return null;
                    values.put(entry.getKey(), string);
                }
                Resource type = ResourceFactory.createResource(ut.getType());
                String expanded = builder.buildFromMap(values).normalize().toASCIIString();
                return new UriTemplateMatch(ut, expanded, type);
            }).filter(Objects::nonNull);
        }

        private String toString(RDFNode node) {
            if (node.isURIResource())  return node.asResource().getURI();
            else if (node.isLiteral()) return node.asLiteral() .getLexicalForm();
            else                       return null;
        }
    }
}
