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

    default int getContigId() {
        return getContig().getId();
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

    GenomicRegion withStrand(Strand strand);

    default GenomicRegion toOppositeStrand() {
        return withStrand(getStrand().getOpposite());
    }

    /**
     * @param other chromosomal region
     * @return true if the region shares at least 1 bp with the <code>other</code> region
     */
    default boolean overlapsWith(GenomicRegion other) {
        if (getContigId() != other.getContigId()) {
            return false;
        }
        GenomicRegion onStrand = other.withStrand(this.getStrand());
        return getStartPosition() <= onStrand.getEndPosition() && getEndPosition() >= onStrand.getStartPosition();
    }

    /**
     * @param other chromosomal region
     * @return true if the <code>other</code> region is fully contained within this region
     */
    default boolean contains(GenomicRegion other) {
        if (this.getContigId() != other.getContigId()) {
            return false;
        }
        GenomicRegion onStrand = other.withStrand(this.getStrand());
        return onStrand.getStartPosition() >= getStartPosition() && onStrand.getEndPosition() <= getEndPosition();
    }

    default boolean contains(GenomicPosition position) {
        if (this.getContigId() != position.getContigId()) {
            return false;
        }
        GenomicPosition onStrand = position.withStrand(this.getStrand());
        return getStartPosition() <= onStrand.getPosition() && onStrand.getPosition() <= getEndPosition();
    }

    /**
     * Get how many 1bp-long steps must be made on the current strand in order to arrive to the first/last coordinate
     * of this region.
     * <p>
     * Note that the positions <em>must</em> be located on the same contig.
     *
     * @param position genome position
     * @return distance as number of 1bp-long steps
     */
    default int differenceTo(GenomicPosition position) {
        int startDistance = getStart().distanceTo(position);
        int endDistance = getEnd().distanceTo(position);

        // return the closest value
        return Math.abs(startDistance) < Math.abs(endDistance)
                ? startDistance
                : endDistance;
    }

    @Override
    default int compareTo(GenomicRegion o) {
        return NATURAL_COMPARATOR.compare(this, o);
    }
}
