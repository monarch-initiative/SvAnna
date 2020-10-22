package org.jax.svann.reference;

import org.jax.svann.reference.genome.Contig;

/**
 * Region on a chromosome that represents a single chromosomal position by default.
 */
public interface ChromosomalRegion extends Comparable<ChromosomalRegion> {

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
        final int contig = getContig().compareTo(o.getContig());
        if (contig != 0) {
            return contig;
        }
        final int begin = getBeginPosition().compareTo(o.getBeginPosition());
        if (begin != 0) {
            return begin;
        }
        final int end = getEndPosition().compareTo(o.getEndPosition());
        if (end != 0) {
            return end;
        }
        if (getStrand().equals(o.getStrand())) {
            return 0;
        } else {
            if (getStrand().equals(Strand.FWD)) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
