package org.jax.svanna.enhancer;

import java.util.List;

public interface IngestedEnhancer {

    String getName();
    /** @return the chromosome (or other comparable contig). */
    String getChromosome();

    int getBegin();

    int getEnd();

    List<AnnotatedTissue> getTissues();

}
