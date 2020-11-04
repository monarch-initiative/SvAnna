
package org.jax.svann.reference;


import org.jax.svann.reference.genome.Contig;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public interface GenomicRegion extends CoordinatePair, Comparable<GenomicRegion> {

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

}
