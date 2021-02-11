package org.jax.svanna.core.annotation;

import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.RepetitiveRegion;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;

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

    List<RepetitiveRegion> overlappingRepetitiveRegions(GenomicRegion query);

}
