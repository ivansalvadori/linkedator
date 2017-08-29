package br.ufsc.inf.lapesd.linkedator.templates;

import br.ufsc.inf.lapesd.linkedator.SemanticMicroserviceDescription;
import br.ufsc.inf.lapesd.linkedator.UriTemplate;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link UriTemplate} with the resource type and a reference to the parent
 * {@link SemanticMicroserviceDescription}.
 */
public class ExtendedUriTemplate extends UriTemplate {
    private String type;
    private @Nonnull SemanticMicroserviceDescription parent;

    public ExtendedUriTemplate(@Nonnull UriTemplate uriTemplate, String type,
                               @Nonnull SemanticMicroserviceDescription parent) {
        this.type = type;
        this.parent = parent;

        setMethod(uriTemplate.getMethod());
        setUri(uriTemplate.getUri());
        Map<String, String> parameters = new HashMap<>();
        parameters.putAll(uriTemplate.getParameters());
        setParameters(parameters);
    }

    @Nonnull
    public SemanticMicroserviceDescription getParent() {
        return parent;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
