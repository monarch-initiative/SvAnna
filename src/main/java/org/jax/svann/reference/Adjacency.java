package org.jax.svann.reference;

/**
 * Adjacency ties together two breakends, as described in VCF specs.
 */
public interface Adjacency extends CoordinatePair {

    @Deprecated
    Breakend getLeft();

    @Override
    default Breakend getStart() {
        return getLeft();
    }

    @Deprecated
    Breakend getRight();

    @Override
    default Breakend getEnd() {
        return getRight();
    }

    byte[] getInserted();

    Adjacency withStrand(Strand strand);

    default Strand getStrand() {
        return getLeft().getStrand();
    }

    /**
     * Convert the adjacency to opposite strand no matter what.
     */
    default Adjacency toOppositeStrand() {
        return getStrand().equals(Strand.FWD)
                ? withStrand(Strand.REV)
                : withStrand(Strand.FWD);
    }
}
