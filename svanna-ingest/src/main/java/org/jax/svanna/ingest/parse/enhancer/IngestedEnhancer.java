package org.jax.svanna.ingest.parse.enhancer;

import java.util.List;

// TODO - extends GenomicRegion
public interface IngestedEnhancer {

    String getName();
    /** @return the chromosome (or other comparable contig). */
    String getChromosome();

    int getBegin();

    int getEnd();

    List<AnnotatedTissue> getTissues();

}
