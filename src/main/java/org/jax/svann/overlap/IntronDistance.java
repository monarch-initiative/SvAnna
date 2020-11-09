package org.jax.svann.overlap;

/**
 * This class organizes information about intronic structural variants (SVs that are completely contained within
 * an intron), and stores the number of the intron (1-based) as well as the distances to the upstream and
 * downstream exons
 */
public class IntronDistance {

    private final int intronNumber;
    private final int distanceToUpstreamExon;
    private final int distanceToDownstreamExon;

    public IntronDistance(int intronNumber, int upDistance, int downDistance) {
        this.intronNumber = intronNumber;
        this.distanceToUpstreamExon = upDistance;
        this.distanceToDownstreamExon = downDistance;
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
}
