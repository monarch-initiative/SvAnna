package org.jax.svann.reference;

import java.util.List;

/**
 * General representation of structural as well as small variants.
 * <p>
 * Implementors must ensure that the following invariants are met:
 * <ul>
 *     <li>at least one adjacency is present</li>
 *     <li>adjacencies are sorted in representative order for the variant</li>
 * </ul>
 */
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
     * @return strand of the leftmost position of the rearrangement
     */
    default Strand getLeftmostStrand() {
        return getRightmostBreakend().getStrand();
    }

    /**
     * Get leftmost position of the rearrangement. The position is on the strand that you get by {@link #getLeftmostStrand()}.
     *
     * @return coordinate of the leftmost position of the rearrangement
     */
    default int getLeftmostPosition() {
        return getRightmostBreakend().getBegin();
    }

    default Breakend getLeftmostBreakend() {
        return getAdjacencies().get(0).getLeft();
    }

    /**
     * @return strand of the rightmost position of the rearrangement
     */
    default Strand getRightmostStrand() {
        return getRightmostBreakend().getStrand();
    }

    /**
     * Get rightmost position of the rearrangement. The position is on the strand that you get by {@link #getRightmostStrand()}.
     *
     * @return coordinate of the rightmost position of the rearrangement
     */
    default int getRightmostPosition() {
        return getRightmostBreakend().getBegin();
    }

    default Breakend getRightmostBreakend() {
        int n = getAdjacencies().size();
        return getAdjacencies().get(n - 1).getRight();
    }

}
