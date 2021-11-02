package org.jax.svanna.core.overlap;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.model.gene.Gene;
import org.monarchinitiative.svart.Variant;

import java.util.List;
import java.util.Map;

public interface GeneOverlapper {

    static GeneOverlapper intervalArrayOverlapper(Map<Integer, IntervalArray<Gene>> geneIntervalArrays) {
        return new IntervalArrayGeneOverlapper(geneIntervalArrays);
    }

    List<GeneOverlap> getOverlaps(Variant variant);

}
