
package org.jax.svann.reference;


import org.jax.svann.reference.genome.Contig;

import java.util.Comparator;

/**
 * A continuous region located on a single contig and on a single strand.
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public interface GenomicRegion extends CoordinatePair, Comparable<GenomicRegion> {

    Comparator<GenomicRegion> NATURAL_COMPARATOR = Comparator.comparing(GenomicRegion::getContig)
            .thenComparing(GenomicRegion::getStart)
            .thenComparing(GenomicRegion::getEnd)
            .thenComparing(GenomicRegion::getStrand)
            .thenComparing(GenomicRegion::getStartCi)
            .thenComparing(GenomicRegion::getEndCi);

    default Contig getContig() {
        return getStart().getContig();
    }

    //    default int getStartMin() {
//        return getStartCi().getMinPos(getStartPosition());
//    }

//    default int getStartMax() {
//        return getStartCi().getMaxPos(getStartPosition());
//    }

    //    default int getEndMin() {
//        return getEndCi().getMinPos(getEndPosition());
//    }

//    default int getEndMax() {
//        return getEndCi().getMaxPos(getEndPosition());
//    }

    default Strand getStrand() {
        return getStart().getStrand();
    }

    @Override
    default int compareTo(GenomicRegion o) {
        return NATURAL_COMPARATOR.compare(this, o);
    }
}
