package org.jax.svann.reference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    default List<CoordinatePair> getRegions() {
        if (getAdjacencies().size() == 1) {
            // cast to CoordinatePair
            return getAdjacencies().stream()
                    .map(a -> ((CoordinatePair) a))
                    .collect(Collectors.toList());
        }
        List<CoordinatePair> regions = new ArrayList<>();

        GenomicPosition previous = null;
        for (int i = 0; i < getAdjacencies().size(); i++) {
            Adjacency current = getAdjacencies().get(i);
            if (current.isInterChromosomal()) {
                regions.add(current);
            }
            if (previous == null) {
                previous = current.getEnd();
                continue;
            }
//            CoordinatePair pair = SimpleCoordinatePair.of(previous)
        }

        return regions;
    }

}
