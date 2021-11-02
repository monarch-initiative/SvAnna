package org.jax.svanna.core.service;

import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.repeat.RepetitiveRegion;
import org.jax.svanna.model.landscape.tad.TadBoundary;
import org.jax.svanna.model.landscape.variant.PopulationVariant;
import org.jax.svanna.model.landscape.variant.PopulationVariantOrigin;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;
import java.util.Set;

/**
 * Convenience interface acting as one-stop-shop for getting genomic annotations.
 */
public interface AnnotationDataService {

    List<Enhancer> overlappingEnhancers(GenomicRegion query);

    Set<TermId> enhancerPhenotypeAssociations();

    List<RepetitiveRegion> overlappingRepetitiveRegions(GenomicRegion query);

    Set<PopulationVariantOrigin> availableOrigins();

    List<PopulationVariant> overlappingPopulationVariants(GenomicRegion query, Set<PopulationVariantOrigin> origins);

    default List<PopulationVariant> allOverlappingPopulationVariants(GenomicRegion query) {
        return overlappingPopulationVariants(query, availableOrigins());
    }

    List<TadBoundary> overlappingTadBoundaries(GenomicRegion query);
}
