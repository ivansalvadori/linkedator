package br.ufsc.inf.lapesd.linkedator.templates;

import br.ufsc.inf.lapesd.linkedator.UriTemplate;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import javax.annotation.Nonnull;

/**
 * A Match against a {@link UriTemplate} includes match information as well as the
 * filled in template (getURI()).
 */
public class UriTemplateMatch {
    private @Nonnull final UriTemplate uriTemplate;
    private @Nonnull final String expanded;
    private final Resource type;

    public UriTemplateMatch(@Nonnull UriTemplate uriTemplate, @Nonnull String expanded,
                            Resource type) {
        this.uriTemplate = uriTemplate;
        this.expanded = expanded;
        this.type = type;
    }

    @Nonnull
    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    public @Nonnull String getUri() {
        return expanded;
    }
    public @Nonnull Resource getResource() {
        return ResourceFactory.createResource(getUri());
    }

    public Resource getType() {
        return type;
    }
}
