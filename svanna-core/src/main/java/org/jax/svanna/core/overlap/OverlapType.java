package org.jax.svanna.core.overlap;

import java.util.Set;

/**
 * Categories to represent how an SV overlaps with a gene.
 * The categories are meant to be used with any type of gene, coding or non-coding, disease-related or not.
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
    DOWNSTREAM_GENE_VARIANT_500KB("500kb downstream gene variant", DOWNSTREAM_GENE_VARIANT),
    /**
     * 500KB_downstream_variant (no SO term)
     */
    UPSTREAM_GENE_VARIANT_500KB("500kb upstream gene variant", UPSTREAM_GENE_VARIANT),
    /**
     * 5KB_downstream_variant (SO:0001633)
     */
    DOWNSTREAM_GENE_VARIANT_5KB("5kb downstream gene variant", DOWNSTREAM_GENE_VARIANT),
    /**
     * 5KB_upstream_variant (SO:0001635)
     */
    UPSTREAM_GENE_VARIANT_5KB("5kb upstream gene variant", UPSTREAM_GENE_VARIANT),
    /**
     * 2KB_downstream_variant (SO:0002083)
     */
    DOWNSTREAM_GENE_VARIANT_2KB("2kb downstream gene variant", DOWNSTREAM_GENE_VARIANT),
    /**
     * 2KB_upstream_variant (SO:0001636)
     */
    UPSTREAM_GENE_VARIANT_2KB("2kb upstream gene variant", UPSTREAM_GENE_VARIANT),
    /**
     * 500B_downstream_variant (SO:0001634)
     */
    DOWNSTREAM_GENE_VARIANT_500B("500b downstream gene variant", DOWNSTREAM_GENE_VARIANT),
    UPSTREAM_GENE_VARIANT_500B("500b upstream gene variant", UPSTREAM_GENE_VARIANT),
    GENIC("affecting a gene"),
    INTRONIC("located completely within intron", GENIC),
    AFFECTS_CODING_TRANSCRIPT_TSS("affects TSS of a coding transcript", GENIC),
    AFFECTS_NONCODING_TRANSCRIPT_TSS("affects TSS of a non-coding transcript", GENIC),
    SINGLE_EXON_IN_TRANSCRIPT("single-exon in coding transcript", GENIC),
    NON_CDS_REGION_IN_SINGLE_EXON("non-coding region of single exon in coding transcript", SINGLE_EXON_IN_TRANSCRIPT),
    SINGLE_EXON_IN_NC_TRANSCRIPT("single-exon in non-coding transcript", SINGLE_EXON_IN_TRANSCRIPT),
    MULTIPLE_EXON_IN_TRANSCRIPT("multiple exons affected in transcript", GENIC),
    TRANSCRIPT_CONTAINED_IN_SV("transcript contained in SV", GENIC),
    TRANSCRIPT_DISRUPTED_BY_TRANSLOCATION("transcript disrupted by translocation", GENIC),
    ENHANCER_DISRUPTED_BY_TRANSLOCATION("enhancers disrupted by translocation", GENIC),
    TRANSLOCATION_WITHOUT_TRANSCRIPT_DISRUPTION("translocation without transcript disruption", GENIC),
    TRANSCRIPT_DISRUPTED_BY_INVERSION("transcript disrupted by inversion", GENIC);

    private final static Set<OverlapType> intergenicTypes = Set.of(
            DOWNSTREAM_GENE_VARIANT, DOWNSTREAM_GENE_VARIANT_500KB, DOWNSTREAM_GENE_VARIANT_5KB, DOWNSTREAM_GENE_VARIANT_2KB, DOWNSTREAM_GENE_VARIANT_500B,
            UPSTREAM_GENE_VARIANT, UPSTREAM_GENE_VARIANT_500KB, UPSTREAM_GENE_VARIANT_5KB, UPSTREAM_GENE_VARIANT_2KB, UPSTREAM_GENE_VARIANT_500B);
    private final static Set<OverlapType> exonicTypes = Set.of(
            SINGLE_EXON_IN_TRANSCRIPT, MULTIPLE_EXON_IN_TRANSCRIPT,
            AFFECTS_CODING_TRANSCRIPT_TSS, AFFECTS_NONCODING_TRANSCRIPT_TSS,
            TRANSCRIPT_CONTAINED_IN_SV);
    private final static Set<OverlapType> intronicTypes = Set.of(INTRONIC);
    private final static Set<OverlapType> upstreamTypes = Set.of(UPSTREAM_GENE_VARIANT, UPSTREAM_GENE_VARIANT_500KB, UPSTREAM_GENE_VARIANT_5KB, UPSTREAM_GENE_VARIANT_2KB, UPSTREAM_GENE_VARIANT_500B);
    private final static Set<OverlapType> downstreamTypes = Set.of(DOWNSTREAM_GENE_VARIANT, DOWNSTREAM_GENE_VARIANT_500KB, DOWNSTREAM_GENE_VARIANT_5KB, DOWNSTREAM_GENE_VARIANT_2KB, DOWNSTREAM_GENE_VARIANT_500B);

    private final String name;

    private final OverlapType baseType;

    OverlapType(String name) {
        this.name = name;
        baseType = this;
    }


    OverlapType(String name, OverlapType baseType) {
        this.name = name;
        this.baseType = baseType;
    }

    public OverlapType baseType() {
        return baseType;
    }

    public static boolean isIntronic(OverlapType type) {
        return intronicTypes.contains(type);
    }

    public static boolean isIntergenic(OverlapType type) {
        return intergenicTypes.contains(type);
    }

    /**
     * Check if this overlap type overlaps with any part of a transcript
     *
     * @param vtype an overlap type
     * @return true if there is overlap with some part of a transcript
     */
    public static boolean overlapsTranscript(OverlapType vtype) {
        return exonicTypes.contains(vtype) || intronicTypes.contains(vtype);
    }

    public static boolean inversionDisruptable(OverlapType vtype) {
        return exonicTypes.contains(vtype) || intronicTypes.contains(vtype) || vtype == UPSTREAM_GENE_VARIANT_2KB;
    }

    public boolean translocationDisruptable() {
        return exonicTypes.contains(this) || intronicTypes.contains(this) || this == UPSTREAM_GENE_VARIANT_2KB;
    }

    public boolean isUpstream() {
        return upstreamTypes.contains(this);
    }

    public boolean isDownstream() {
        return downstreamTypes.contains(this);
    }

    public boolean isExonic() {
        return exonicTypes.contains(this);
    }

    public boolean isSingleExon() {
        return this == SINGLE_EXON_IN_TRANSCRIPT;
    }

    public boolean isIntronic() {
        return isIntronic(this);
    }

    public boolean isIntergenic() {
        return isIntergenic(this);
    }

    public String getName() {
        return name;
    }


}
