package br.ufsc.inf.lapesd.linkedator;

import br.ufsc.inf.lapesd.linkedator.links.LinkVerifier;
import br.ufsc.inf.lapesd.linkedator.links.NullLinkVerifier;
import org.apache.jena.rdf.model.Model;

import javax.annotation.Nonnull;

/**
 * Creates links according to known ontologies and service descriptions.
 */
public interface Linkedator {
    /**
     * Infer possible links for resources in model and add them directly in model.
     *
     * @param model Model where links will be created
     * @param linkVerifier used to check if links are reachable. Only reachable links are
     *                     added. Use {@link NullLinkVerifier} to disable validation.
     */
    void createLinks(@Nonnull Model model, @Nonnull LinkVerifier linkVerifier);

    /**
     * Replaces the ontologies model used for reasoning and link creation with the one provided.
     * This model should contain the union graph of all OWL ontologies and RDFS vocabularies
     * that Linkedator should consider in link creation.
     */
    void updateOntologies(@Nonnull Model model);

    /**
     * Adds all triples in model to the union ontologies model. Triples are copied, therefore
     * future changes to model will not be visible to Linkedator.
     */
    void addToOntologies(@Nonnull Model model);

    /**
     * Register the given service documentation for use in link generation.
     */
    void register(@Nonnull SemanticMicroserviceDescription semanticMicroserviceDescription);
}
