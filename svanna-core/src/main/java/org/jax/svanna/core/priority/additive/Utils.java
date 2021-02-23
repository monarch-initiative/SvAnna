package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.Comparator;

class Utils {

    static Comparator<? extends GenomicRegion> regionOnPositiveStrand() {
        return Comparator.comparing(GenomicRegion::contig)
                .thenComparingInt(r -> r.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                .thenComparingInt(r -> r.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
    }



}
