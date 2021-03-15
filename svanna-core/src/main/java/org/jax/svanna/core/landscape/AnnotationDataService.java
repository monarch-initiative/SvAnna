package org.jax.svanna.core.landscape;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;
import java.util.Set;

/**
 * Convenience class acting as one-stop-shop for getting genomic annotations.
 */
public interface AnnotationDataService
//        extends TranscriptService // TODO - implement
{

    // TODO - implement
//    QueryResult<Transcript> overlappingTranscript(GenomicRegion query);

    List<Enhancer> overlappingEnhancers(GenomicRegion query);

    @Deprecated
        // use overlappingEnhancers instead
    List<Enhancer> allEnhancers();

    Set<TermId> enhancerPhenotypeAssociations();

    List<RepetitiveRegion> overlappingRepetitiveRegions(GenomicRegion query);

    Set<PopulationVariantOrigin> availableOrigins();

    List<PopulationVariant> overlappingPopulationVariants(GenomicRegion query, Set<PopulationVariantOrigin> origins);

    default List<PopulationVariant> overlappingPopulationVariants(GenomicRegion query) {
        return overlappingPopulationVariants(query, availableOrigins());
    }

    List<TadBoundary> overlappingTadBoundaries(GenomicRegion query);
}
