package org.jax.svann.reference;

/**
 * Adjacency ties together two breakends, as described in VCF specs.
 */
public interface Adjacency {

    Breakend getLeft();

    Breakend getRight();

    Adjacency withStrand(Strand strand);

    default Strand getStrand() {
        return getLeft().getStrand();
    }
}
