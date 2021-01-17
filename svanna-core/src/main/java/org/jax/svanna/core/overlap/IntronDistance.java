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

    public String getUpDownStreamDistance(boolean posStrand) {
        if (posStrand) {
            int exonUp = intronNumber; // upstream exon in chromosomal coordinates
            int exonDown = intronNumber + 1; // downstream exon in chromosomal coordinates
            return String.format("intron %d; %d bp to exon %d; %d bp to exon %d",
                    intronNumber,
                    Math.abs(this.distanceToUpstreamExon),
                    exonUp,
                    Math.abs(this.distanceToDownstreamExon),
                    exonDown);
        } else {
            int exonUp = intronNumber + 1; // upstream exon in chromosomal coordinates
            int exonDown = intronNumber; // downstream exon in chromosomal coordinates
            return String.format("intron %d; %d bp to exon %d; %d bp to exon %d",
                    intronNumber,
                    Math.abs(this.distanceToDownstreamExon),
                    exonUp,
                    Math.abs(this.distanceToUpstreamExon),
                    exonDown);
        }
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
