package org.jax.svann.overlap;

import org.jax.svann.priority.SvImpact;

import java.util.Set;

/**
 * Categories to represent how an SV overlaps with a gene.
 * The categories are meant to be used with any type of gene, coding or non-coding, disease-related or not.
 * <p>
 * In general, the higher ordinal means the higher deleteriousness.
 *
 * @author Peter N Robinson
 */
public enum OverlapType {
    UNKNOWN("unknown"),
    /**
     * downstream_gene_variant (SO:0001632)
     */
    DOWNSTREAM_GENE_VARIANT("downstream gene variant"),
    /**
     * upstream_gene_variant (SO:0001631)
     */
    UPSTREAM_GENE_VARIANT("upstream gene variant"),
    /**
     * 500KB_downstream_variant (no SO term)
     */
    DOWNSTREAM_GENE_VARIANT_500KB("500kb downstream gene variant"),
    /**
     * 500KB_downstream_variant (no SO term)
     */
    UPSTREAM_GENE_VARIANT_500KB("500kb upstream gene variant"),
    /**
     * 5KB_downstream_variant (SO:0001633)
     */
    DOWNSTREAM_GENE_VARIANT_5KB("5kb downstream gene variant"),
    /**
     * 5KB_upstream_variant (SO:0001635)
     */
    UPSTREAM_GENE_VARIANT_5KB("5kb upstream gene variant"),
    /**
     * 2KB_downstream_variant (SO:0002083)
     */
    DOWNSTREAM_GENE_VARIANT_2KB("2kb downstream gene variant"),
    /**
     * 2KB_upstream_variant (SO:0001636)
     */
    UPSTREAM_GENE_VARIANT_2KB("2kb upstream gene variant"),
    /**
     * 500B_downstream_variant (SO:0001634)
     */
    DOWNSTREAM_GENE_VARIANT_500B("500b downstream gene variant"),
    UPSTREAM_GENE_VARIANT_500B("500b upstream gene variant"),
    INTRONIC("located completely within intron"),
    SINGLE_EXON_IN_TRANSCRIPT("single-exon affected in transcript"),
    MULTIPLE_EXON_IN_TRANSCRIPT("multiple exons affected in transcript"),
    TRANSCRIPT_CONTAINED_IN_SV("transcript contained in SV"),
    TRANSCRIPT_DISRUPTED_BY_TRANSLOCATION("transcript disrupted by translocation"),
    TRANSCRIPT_DISRUPTED_BY_INVERSION("transcript disrupted by inversion");

    private final static Set<OverlapType> intergenicTypes = Set.of(DOWNSTREAM_GENE_VARIANT, DOWNSTREAM_GENE_VARIANT_2KB, DOWNSTREAM_GENE_VARIANT_5KB,
            DOWNSTREAM_GENE_VARIANT_500B, UPSTREAM_GENE_VARIANT, UPSTREAM_GENE_VARIANT_2KB, UPSTREAM_GENE_VARIANT_5KB, UPSTREAM_GENE_VARIANT_500KB);
    private final static Set<OverlapType> exonicTypes = Set.of(SINGLE_EXON_IN_TRANSCRIPT, MULTIPLE_EXON_IN_TRANSCRIPT, TRANSCRIPT_CONTAINED_IN_SV);
    private final static Set<OverlapType> intronicTypes = Set.of(INTRONIC);
    private final static Set<OverlapType> upstreamTypes = Set.of(UPSTREAM_GENE_VARIANT, UPSTREAM_GENE_VARIANT_500B, UPSTREAM_GENE_VARIANT_2KB, UPSTREAM_GENE_VARIANT_5KB, UPSTREAM_GENE_VARIANT_500KB);
    private final static Set<OverlapType> downstreamTypes = Set.of(DOWNSTREAM_GENE_VARIANT, DOWNSTREAM_GENE_VARIANT_500B, DOWNSTREAM_GENE_VARIANT_2KB, DOWNSTREAM_GENE_VARIANT_5KB, DOWNSTREAM_GENE_VARIANT_500KB);

    private final String name;


    OverlapType(String type) {
        name = type;
    }


    public static boolean isIntronic(OverlapType type) {
        return intronicTypes.contains(type);
    }

    public static boolean isIntergenic(OverlapType type) {
        return intergenicTypes.contains(type);
    }

    public boolean isUpstream() { return upstreamTypes.contains(this); }
    public boolean isDownstream() { return downstreamTypes.contains(this); }

    public boolean isExonic() {
        return exonicTypes.contains(this);
    }

    public boolean isSingleExon() { return this == SINGLE_EXON_IN_TRANSCRIPT; }

    public boolean isIntronic() {
        return isIntronic(this);
    }

    public boolean isIntergenic() {
        return isIntergenic(this);
    }

    /**
     * Check if this overlap type overlaps with any part of a transcript
     * @param vtype an overlap type
     * @return true if there is overlap with some part of a transcript
     */
    public static boolean overlapsTranscript(OverlapType vtype) {
        return exonicTypes.contains(vtype) || intronicTypes.contains(vtype);
    }

    public static boolean inversionDisruptable(OverlapType vtype) {
        return exonicTypes.contains(vtype) || intronicTypes.contains(vtype) || vtype == UPSTREAM_GENE_VARIANT_2KB;
    }

    public String getName() {
        return name;
    }

    public SvImpact defaultSvImpact() {
        switch (this) {
            case SINGLE_EXON_IN_TRANSCRIPT:
            case MULTIPLE_EXON_IN_TRANSCRIPT:
            case TRANSCRIPT_CONTAINED_IN_SV:
            case UPSTREAM_GENE_VARIANT_2KB:
            case DOWNSTREAM_GENE_VARIANT_2KB:
            case TRANSCRIPT_DISRUPTED_BY_INVERSION:
                return SvImpact.HIGH;
            case UPSTREAM_GENE_VARIANT_5KB:
            case DOWNSTREAM_GENE_VARIANT_5KB:
                return SvImpact.INTERMEDIATE;
            case INTRONIC:
            default:
                return SvImpact.LOW;
        }
    }


}
