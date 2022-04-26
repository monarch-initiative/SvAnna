package org.monarchinitiative.svanna.core.overlap;

/**
 * This class encapsulates information about the distance of a structural variant to transcripts or
 * enhancers. In many cases, we have two distances (e.g., an intronic SV has a certain distance to
 * the upstream and to the downstream exon). In other cases, we have only one distance (e.g., upstream
 * and downstream).
 *
 * @author Peter N Robinson
 */
public class OverlapDistance {

    private static final int UNITIALIZED = Integer.MIN_VALUE;

    private final OverlapDistanceType overlapDistanceType;

    private final int upstreamDistance;
    private final int downstreamDistance;
    private final boolean overlapsCds;
    private final String description;

    private OverlapDistance(OverlapDistanceType odtype, int distance, boolean overlapsCds, String description) {
        this(odtype, distance, UNITIALIZED, overlapsCds, description);
    }

    private OverlapDistance(OverlapDistanceType odtype, int upstreamDistance, int downstreamDistance, boolean overlapsCds, String description) {
        overlapDistanceType = odtype;
        this.upstreamDistance = upstreamDistance;
        this.downstreamDistance = downstreamDistance;
        this.description = description;
        this.overlapsCds = overlapsCds;
    }

    public enum OverlapDistanceType {INTERGENIC, INTRONIC, EXONIC, CONTAINED_IN}

    public OverlapDistanceType overlapDistanceType() {
        return overlapDistanceType;
    }

    public boolean overlapsCds() {
        return overlapsCds;
    }

    public String getDescription() {
        return description;
    }

    public int getShortestDistance() {
        if (downstreamDistance == UNITIALIZED) {
            return upstreamDistance;
        } else {
            return Math.abs(upstreamDistance) < Math.abs(downstreamDistance)
                    ? upstreamDistance
                    : downstreamDistance;
        }
    }

    public static OverlapDistance fromUpstreamFlankingGene(int upstreamDistance, String geneSymbol) {
        String description = String.format("Intergenic: %s upstream of %s", distanceString(upstreamDistance), geneSymbol);
        return new OverlapDistance(OverlapDistanceType.INTERGENIC, upstreamDistance, false, description);
    }

    public static OverlapDistance fromDownstreamFlankingGene(int downstreamDistance, String geneSymbol) {
        String description = String.format("Intergenic: %s downstream of %s", distanceString(downstreamDistance), geneSymbol);
        return new OverlapDistance(OverlapDistanceType.INTERGENIC, downstreamDistance, false, description);
    }

    /**
     * If an SV overlaps an exon, then we classify it as having a distance of zero.
     *
     * @param geneSymbol symbol of the overlapped gene
     * @return OverlapDistance object to signify zero distance to a transcript (exon)
     */
    public static OverlapDistance fromExonic(String geneSymbol, boolean overlapsCds) {
        String description = "Exonic"; // we do not use the OverlapDistance to provide descriptions about exonic events
        return new OverlapDistance(OverlapDistanceType.EXONIC, 0, overlapsCds, description);
    }

    public static OverlapDistance fromIntronic(String geneSymbol, IntronDistance idistance) {
        String description = String.format("Intronic: %s and %s removed from flanking exons of %s",
                distanceString(Math.abs(idistance.getDistanceToUpstreamExon())), distanceString(idistance.getDistanceToDownstreamExon()), geneSymbol);
        return new OverlapDistance(OverlapDistanceType.INTRONIC,
                idistance.getDistanceToUpstreamExon(),
                idistance.getDistanceToDownstreamExon(), false, description);
    }

    public static OverlapDistance fromContainedIn() {
        return new OverlapDistance(OverlapDistanceType.CONTAINED_IN, 0, true, "");
    }

    private static String distanceString(int d) {
        d = Math.abs(d);
        if (d < 1_000) {
            return String.format("%d bp", d);
        } else if (d < 1_000_000) {
            double x = (double) d / 1_000.0;
            return String.format("%.2f kb", x);
        } else {
            double x = (double) d / 1_000_000.0;
            return String.format("%.2f Mb", x);
        }
    }
}
