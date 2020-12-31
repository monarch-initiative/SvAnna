package org.jax.svanna.core.overlap;

/**
 * This class encapsulates information about the distance of a structural variant to transcripts or
 * enhancers. In many cases, we have two distances (e.g., an intronic SV has a certain distance to
 * the upstream and to the downstream exon). In other cases, we have only one distance (e.g., upstream
 * and downstream).
 *
 * @author Peter N Robinson
 */
public class OverlapDistance {

    private static final int UNITIALIZED = -42;
    private final OverlapDistanceType overlapDistanceType;
    private final int distanceA;
    private final int distanceB;
    private final boolean overlapsCds;
    private final String description;

    private OverlapDistance(OverlapDistanceType odtype, int distance, String description, boolean cds) {
        this(odtype, distance, UNITIALIZED, description, cds);
    }

    private OverlapDistance(OverlapDistanceType odtype, int distancea, int distanceb, String description, boolean cds) {
        overlapDistanceType = odtype;
        this.distanceA = distancea;
        this.distanceB = distanceb;
        this.description = description;
        this.overlapsCds = cds;
    }

    public static OverlapDistance fromUpstreamFlankingGene(int distance, String geneSymbol) {
        String description = String.format("Intergenic: %s upstream of %s", distanceString(distance), geneSymbol);
        return new OverlapDistance(OverlapDistanceType.INTERGENIC, distance, description, false);
    }

    public static OverlapDistance fromDownstreamFlankingGene(int distance, String geneSymbol) {
        String description = String.format("Intergenic: %s downstream of %s", distanceString(distance), geneSymbol);
        return new OverlapDistance(OverlapDistanceType.INTERGENIC, distance, description, false);
    }

    /**
     * If an SV overlaps an exon, then we classify it as having a distance of zero.
     *
     * @param geneSymbol symbol of the overlapped gene
     * @return OverlapDistance object to signify zero distance to a transcript (exon)
     */
    public static OverlapDistance fromExonic(String geneSymbol, boolean overlapsCds) {
        String description = "Exonic"; // we do not use the OverlapDistance to provide descriptions about exonic events
        return new OverlapDistance(OverlapDistanceType.EXONIC, 0, description, overlapsCds);
    }

    public static OverlapDistance fromIntronic(String geneSymbol, IntronDistance idistance) {
        String description = String.format("Intronic: %s and %s removed from flanking exons of %s",
                distanceString(Math.abs(idistance.getDistanceToUpstreamExon())), distanceString(idistance.getDistanceToDownstreamExon()), geneSymbol);
        return new OverlapDistance(OverlapDistanceType.INTRONIC,
                idistance.getDistanceToUpstreamExon(),
                idistance.getDistanceToDownstreamExon(), description, false);
    }

    public static OverlapDistance fromContainedIn() {
        return new OverlapDistance(OverlapDistanceType.CONTAINED_IN, 0, "", true);
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

    public boolean isOverlapsCds() {
        return overlapsCds;
    }

    public String getDescription() {
        return description;
    }

    public int getShortestDistance() {
        if (distanceB == UNITIALIZED) {
            return distanceA;
        } else {
            return Math.abs(distanceA) < Math.abs(distanceB)
                    ? distanceA
                    : distanceB;
        }
    }

    enum OverlapDistanceType {INTERGENIC, INTRONIC, EXONIC, CONTAINED_IN}
}
