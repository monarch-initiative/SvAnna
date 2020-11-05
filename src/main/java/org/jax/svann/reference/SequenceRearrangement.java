package org.jax.svann.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

    Logger LOGGER = LoggerFactory.getLogger(SequenceRearrangement.class);

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
        return getLeftmostBreakend().getStrand();
    }

    /**
     * Get leftmost position of the rearrangement. The position is on the strand that you get by {@link #getLeftmostStrand()}.
     *
     * @return coordinate of the leftmost position of the rearrangement
     */
    default int getLeftmostPosition() {
        return getLeftmostBreakend().getPosition();
    }

    default Breakend getLeftmostBreakend() {
        return getAdjacencies().get(0).getStart();
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
        return getRightmostBreakend().getPosition();
    }

    default Breakend getRightmostBreakend() {
        int n = getAdjacencies().size();
        return getAdjacencies().get(n - 1).getEnd();
    }

    /**
     * Get coordinate pairs representing the rearranged regions. The coordinate pairs be intrachromosomal as well as
     * interchromosomal if translocation is present.
     *
     * @return list with coordinate pairs
     */
    default List<CoordinatePair> getRegions() {
        if (getType().equals(SvType.INSERTION)) {
            // insertion is a special creature
            return List.of(SimpleCoordinatePair.of(getLeftmostBreakend(), getRightmostBreakend()));
        }

        int nAdjacencies = getAdjacencies().size();
        boolean evenNumberOfAdjacencies = nAdjacencies % 2 == 0;

        // gather all breakends
        List<Breakend> breakends = new ArrayList<>(nAdjacencies * 2);
        for (Adjacency adjacency : getAdjacencies()) {
            breakends.add(adjacency.getStart());
            breakends.add(adjacency.getEnd());
        }

        List<CoordinatePair> pairs = new ArrayList<>();
        Breakend previous = null;
        // we start iteration depending on whether we're dealing with even or odd number of adjacencies
        int iStart = evenNumberOfAdjacencies ? 1 : 0;
        for (int i = iStart; i < breakends.size(); i++) {
            Breakend breakend = breakends.get(i);
            if (previous == null) {
                previous = breakend;
            } else {
                SimpleCoordinatePair pair = SimpleCoordinatePair.of(previous, breakend);
                pairs.add(pair);
                previous = null;
            }
        }

        return pairs;
    }

}
