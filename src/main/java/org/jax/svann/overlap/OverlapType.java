package org.jax.svann.overlap;

import htsjdk.samtools.cram.encoding.core.huffmanUtils.HuffmanIntHelper;
import org.jax.svann.priority.SvImpact;

import java.util.Set;

/**
 * Categories to represent how an SV overlaps with a gene.
 * The categories are meant to be used with any type of gene, coding or non-coding, disease-related or not.
 *
 * @author Peter N Robinson
 */
public enum OverlapType {
    /**
     * downstream_gene_variant (SO:0001632)
     */
    DOWNSTREAM_GENE_VARIANT("downstream gene variant"),
    /**
     * 2KB_downstream_variant (SO:0002083)
     */
    DOWNSTREAM_GENE_VARIANT_2KB("2kb downstream gene variant"),
    /**
     * 5KB_downstream_variant (SO:0001633)
     */
    DOWNSTREAM_GENE_VARIANT_5KB("5kb downstream gene variant"),
    /**
     * 500KB_downstream_variant (SO:0001634)
     */
    DOWNSTREAM_GENE_VARIANT_500KB("500kb downstream gene variant"),
    /**
     * upstream_gene_variant (SO:0001631)
     */
    UPSTREAM_GENE_VARIANT("upstream gene variant"),
    /**
     * 2KB_upstream_variant (SO:0001636)
     */
    UPSTREAM_GENE_VARIANT_2KB("2kb upstream gene variant"),
    /**
     * 5KB_upstream_variant (SO:0001635)
     */
    UPSTREAM_GENE_VARIANT_5KB("5kb upstream gene variant"),
    /**
     * 500KB_downstream_variant (no SO term)
     */
    UPSTREAM_GENE_VARIANT_500KB("500kb upstream gene variant"),
    SINGLE_EXON_IN_TRANSCRIPT("single-exon affected in transcript"),
    MULTIPLE_EXON_IN_TRANSCRIPT("multiple exons affected in transcript"),
    INTRONIC("located completely within intron"),
    TRANSCRIPT_CONTAINED_IN_SV("transcript contained in SV"),
    TRANSCRIPT_DISRUPTED_BY_INVERSION("transcript disrupted by inversion"),
    UNKNOWN("unknown");

    private final static Set<OverlapType> intergenicTypes = Set.of(DOWNSTREAM_GENE_VARIANT, DOWNSTREAM_GENE_VARIANT_2KB, DOWNSTREAM_GENE_VARIANT_5KB,
            DOWNSTREAM_GENE_VARIANT_500KB, UPSTREAM_GENE_VARIANT, UPSTREAM_GENE_VARIANT_2KB, UPSTREAM_GENE_VARIANT_5KB, UPSTREAM_GENE_VARIANT_500KB);
    private final static Set<OverlapType> exonicTypes = Set.of(SINGLE_EXON_IN_TRANSCRIPT, MULTIPLE_EXON_IN_TRANSCRIPT, TRANSCRIPT_CONTAINED_IN_SV);
    private final static Set<OverlapType> intronicTypes = Set.of(INTRONIC);

    private final String name;


    OverlapType(String type) {
        name = type;
    }

    public static boolean isExonic(OverlapType vtype) {
        return exonicTypes.contains(vtype);
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


    public SvImpact toImpact() {
        switch (this) {
            case SINGLE_EXON_IN_TRANSCRIPT:
            case MULTIPLE_EXON_IN_TRANSCRIPT:
                return SvImpact.HIGH_IMPACT;
            case TRANSCRIPT_CONTAINED_IN_SV:
                return SvImpact.HIGH_IMPACT;
            case UPSTREAM_GENE_VARIANT_2KB:
            case DOWNSTREAM_GENE_VARIANT_2KB:
                return SvImpact.HIGH_IMPACT;
            case UPSTREAM_GENE_VARIANT_5KB:
            case DOWNSTREAM_GENE_VARIANT_5KB:
                return SvImpact.INTERMEDIATE_IMPACT;
            default:
                return SvImpact.LOW_IMPACT;
        }
    }


}
