package org.jax.svann.reference;

import java.util.List;

public interface SequenceRearrangement {

    /**
     * @return structural rearrangement type
     */
    SvType getType();

    /**
     * @return list of adjacencies
     */
    List<Adjacency> getAdjacencies();

    /**
     * Flip the rearrangement to given <code>strand</code>. Flipping reverses order and strand of the adjacencies.
     *
     * @param strand to flip the rearrangement to
     * @return flipped rearrangement
     */
    SequenceRearrangement withStrand(Strand strand);

    /**
     * @return strand of the first adjacency
     */
    default Strand getStrand() {
        return getAdjacencies().isEmpty()
                ? Strand.FWD
                : getAdjacencies().get(0).getStrand();
    }
}
