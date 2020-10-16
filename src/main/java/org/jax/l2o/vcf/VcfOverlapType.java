package org.jax.l2o.vcf;

import java.util.Set;

public enum VcfOverlapType {
//    INTERGENIC, SINGLE_CDS_EXON, MULTIPLE_EXON_ONE_GENE, MULTIPLE_GENE, INTRONIC, NON_CODING_EXON, UTR_EXON,
//    DOWNSTREAM_INTERGENIC, UPSTREAM_INTERGENIC, UNKNOWN;
    /** downstream_gene_variant (SO:0001632) */
    DOWNSTREAM_GENE_VARIANT("downstream gene variant"),
    /** 2KB_downstream_variant (SO:0002083) */
    DOWNSTREAM_GENE_VARIANT_2KB("2kb downstream gene variant"),
    /** 5KB_downstream_variant (SO:0001633) */
    DOWNSTREAM_GENE_VARIANT_5KB("5kb downstream gene variant"),
    /** 500KB_downstream_variant (SO:0001634) */
    DOWNSTREAM_GENE_VARIANT_500KB("500kb downstream gene variant"),
    /** upstream_gene_variant (SO:0001631) */
    UPSTREAM_GENE_VARIANT("upstream gene variant"),
    /** 2KB_upstream_variant (SO:0001636) */
    UPSTREAM_GENE_VARIANT_2KB("2kb upstream gene variant"),
    /** 5KB_upstream_variant (SO:0001635) */
    UPSTREAM_GENE_VARIANT_5KB("5kb upstream gene variant"),
    /** 500KB_downstream_variant (no SO term) */
    UPSTREAM_GENE_VARIANT_500KB("500kb upstream gene variant"),
    SINGLE_EXON_NONCODING_TRANSCRIPT("single-exon in non-coding transcript"),
    MULTIPLE_EXON_NONCODING_TRANSCRIPT("multi-exon in non-coding transcript"),
    INTRONIC_NONCODING_TRANSCRIPT("intronic in non-coding transcript"),
    SINGLE_EXON_CODING_TRANSCRIPT("single-exon in non-coding transcript"),
    MULTIPLE_EXON_CODING_TRANSCRIPT("multi-exon in non-coding transcript"),
    INTRONIC_CODING_TRANSCRIPT("intronic in non-coding transcript"),
    UNKNOWN("unknown");


    private final static Set<VcfOverlapType> codingTypes = Set.of(SINGLE_EXON_CODING_TRANSCRIPT, MULTIPLE_EXON_CODING_TRANSCRIPT );

    private String name;


    VcfOverlapType(String typ){name = typ;}

    public static boolean isCoding(VcfOverlapType vtype) {
        return codingTypes.contains(vtype);
    }


}
