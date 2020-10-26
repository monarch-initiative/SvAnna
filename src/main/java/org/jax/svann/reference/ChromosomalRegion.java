package org.jax.svann.reference;

import org.jax.svann.reference.genome.Contig;

import java.util.Comparator;

/**
 * Region on a chromosome that represents a single chromosomal position by default.
 */
public interface ChromosomalRegion extends Comparable<ChromosomalRegion> {

    Comparator<ChromosomalRegion> DEFAULT_COMPARATOR = Comparator.comparing(ChromosomalRegion::getContig)
            .thenComparing(ChromosomalRegion::getBeginPosition)
            .thenComparing(ChromosomalRegion::getEndPosition)
            .thenComparing(ChromosomalRegion::getStrand);

    /**
     * @return contig where the region is located
     */
    Contig getContig();

    /**
     * @return 1-based begin coordinate
     */
    Position getBeginPosition();

    default int getBegin() {
        return getBeginPosition().getPos();
    }

    /**
     * The begin position is also the end by default
     *
     * @return 1-based end coordinate
     */
    default Position getEndPosition() {
        return getBeginPosition();
    }

    default int getEnd() {
        return getEndPosition().getPos();
    }

    Strand getStrand();

    ChromosomalRegion withStrand(Strand strand);

    default int length() {
        return getEnd() - getBegin() + 1;
    }

    @Override
    default int compareTo(ChromosomalRegion o) {
        return DEFAULT_COMPARATOR.compare(this, o);
    }
}
