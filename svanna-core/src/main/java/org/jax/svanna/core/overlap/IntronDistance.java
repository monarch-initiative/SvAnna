package org.jax.svanna.core.overlap;

import java.util.Objects;

/**
 * This class organizes information about intronic structural variants (SVs that are completely contained within
 * an intron), and stores the number of the intron (1-based) as well as the distances to the upstream and
 * downstream exons
 */
public class IntronDistance {

    private static final IntronDistance EMPTY = new IntronDistance(0, 0, 0);

    private final int intronNumber;
    private final int distanceToUpstreamExon;
    private final int distanceToDownstreamExon;

    public IntronDistance(int intronNumber, int upDistance, int downDistance) {
        this.intronNumber = intronNumber;
        this.distanceToUpstreamExon = upDistance;
        this.distanceToDownstreamExon = downDistance;
    }

    public static IntronDistance empty() {
        return EMPTY;
    }

    public int getIntronNumber() {
        return intronNumber;
    }

    public int getDistanceToUpstreamExon() {
        return distanceToUpstreamExon;
    }

    public int getDistanceToDownstreamExon() {
        return distanceToDownstreamExon;
    }

    public boolean isEmpty() {
        return this.equals(EMPTY);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntronDistance that = (IntronDistance) o;
        return intronNumber == that.intronNumber &&
                distanceToUpstreamExon == that.distanceToUpstreamExon &&
                distanceToDownstreamExon == that.distanceToDownstreamExon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(intronNumber, distanceToUpstreamExon, distanceToDownstreamExon);
    }

    @Override
    public String toString() {
        return "IntronDistance{" +
                "intronNumber=" + intronNumber +
                ", distanceToUpstreamExon=" + distanceToUpstreamExon +
                ", distanceToDownstreamExon=" + distanceToDownstreamExon +
                '}';
    }
}
