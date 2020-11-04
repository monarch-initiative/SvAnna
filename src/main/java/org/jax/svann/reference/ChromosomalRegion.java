package org.jax.svann.reference;

import org.jax.svann.reference.genome.Contig;

/**
 * Region on a chromosome that represents a single chromosomal position by default.
 */
@Deprecated // in favor of GenomicPosition
public interface ChromosomalRegion { // extends Comparable<ChromosomalRegion> {

//    Comparator<ChromosomalRegion> DEFAULT_COMPARATOR = Comparator.comparing(ChromosomalRegion::getContig)
//            .thenComparing(ChromosomalRegion::getBeginPosition)
//            .thenComparing(ChromosomalRegion::getEndPosition)
//            .thenComparing(ChromosomalRegion::getStrand);

    /**
     * @return contig where the region is located
     */
    Contig getContig();

    /**
     * @return 1-based begin coordinate
     */
    @Deprecated
    Position getBeginPosition();

    /**
     * @return 1-based begin coordinate of the region
     */
    @Deprecated
    default int getBegin() {
        return getBeginPosition().getPos();
    }

    /**
     * The begin position is also the end by default
     *
     * @return 1-based end coordinate
     */
    @Deprecated
    default Position getEndPosition() {
        return getBeginPosition();
    }

    /**
     * @return 1-based end coordinate of the region
     */
    @Deprecated
    default int getEnd() {
        return getEndPosition().getPos();
    }

//    Strand getStrand();

    ChromosomalRegion withStrand(Strand strand);

    @Deprecated
    default int length() {
        return getEnd() - getBegin() + 1;
    }

//    /**
//     * @param other chromosomal region
//     * @return true if the region shares at least 1 bp with the <code>other</code> region
//     */
//    default boolean overlapsWith(ChromosomalRegion other) {
//        if (this.getContig().getId() != other.getContig().getId()) {
//            return false;
//        }
//        ChromosomalRegion onStrand = other.withStrand(this.getStrand());
//        return getBegin() <= onStrand.getEnd() && getEnd() >= onStrand.getBegin();
//    }
//
//    /**
//     * @param other chromosomal region
//     * @return true if the <code>other</code> region is fully contained within this region
//     */
//    default boolean contains(ChromosomalRegion other) {
//        if (this.getContig().getId() != other.getContig().getId()) {
//            return false;
//        }
//        ChromosomalRegion onStrand = other.withStrand(this.getStrand());
//        return onStrand.getBegin() >= getBegin() && onStrand.getEnd() <= getEnd();
//    }

//    @Override
//    default int compareTo(ChromosomalRegion other) {
//        return DEFAULT_COMPARATOR.compare(this, other);
//    }
}
