package org.jax.svanna.core.overlap;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.monarchinitiative.svart.Variant;
import xyz.ielis.silent.genes.model.Gene;

import java.util.List;
import java.util.Map;

public interface GeneOverlapper {

    static GeneOverlapper intervalArrayOverlapper(Map<Integer, IntervalArray<Gene>> geneIntervalArrays) {
        return new IntervalArrayGeneOverlapper(geneIntervalArrays);
    }

    List<GeneOverlap> getOverlaps(Variant variant);

}
