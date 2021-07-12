package org.jax.svanna.core.landscape;

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
