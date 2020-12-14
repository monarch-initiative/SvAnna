package org.jax.svanna.io.parse;

import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypesContext;
import org.jax.svanna.core.reference.VariantMetadata;
import org.jax.svanna.core.reference.Zygosity;

import java.util.Objects;

/**
 * POJO for grouping variant data required for implementing {@link VariantMetadata}.
 */
class Metadata {
    private static final int[] MISSING_AD = new int[]{VariantMetadata.MISSING_DEPTH_PLACEHOLDER, VariantMetadata.MISSING_DEPTH_PLACEHOLDER};
    private static final Metadata MISSING = new Metadata(Zygosity.UNKNOWN,
            VariantMetadata.MISSING_DEPTH_PLACEHOLDER,
            VariantMetadata.MISSING_DEPTH_PLACEHOLDER,
            VariantMetadata.MISSING_DEPTH_PLACEHOLDER);

    static Metadata missing() {
        return MISSING;
    }
    private final Zygosity zygosity;
    private final int dp, refReads, altReads;
    Metadata(Zygosity zygosity,
                     int dp,
                     int refReads,
                     int altReads) {
        this.zygosity = Objects.requireNonNull(zygosity);

        if (dp < VariantMetadata.MISSING_DEPTH_PLACEHOLDER) {
            throw new IllegalArgumentException("Minimum depth of coverage must be greater than `-1`: " + dp);
        }
        this.dp = dp;

        if (refReads < VariantMetadata.MISSING_DEPTH_PLACEHOLDER) {
            throw new IllegalArgumentException("Number of reads supporting ref allele must be greater than `-1`: " + dp);
        }
        this.refReads = refReads;

        if (altReads < VariantMetadata.MISSING_DEPTH_PLACEHOLDER) {
            throw new IllegalArgumentException("Number of reads supporting alt allele must be greater than `-1`: " + dp);
        }
        this.altReads = altReads;
    }

    static Metadata parseGenotypeData(int sampleIdx, GenotypesContext genotypes) {
        if (genotypes.isEmpty() || sampleIdx >= genotypes.size()) {
            return MISSING;
        }
        Genotype gt = genotypes.get(sampleIdx);
        int dp = gt.hasDP()
                ? gt.getDP()
                : VariantMetadata.MISSING_DEPTH_PLACEHOLDER;

        int[] ad = gt.hasAD() ? gt.getAD() : MISSING_AD;

        Zygosity zygosity = parseZygosity(sampleIdx, genotypes);

        return new Metadata(zygosity, dp, ad[0], ad[1]);
    }

    private static Zygosity parseZygosity(int sampleIdx, GenotypesContext gts) {
        if (gts.isEmpty() || sampleIdx >= gts.size()) {
            return Zygosity.UNKNOWN;
        }
        Genotype gt = gts.get(sampleIdx);
        switch (gt.getType()) {
            case HET:
                return Zygosity.HETEROZYGOUS;
            case HOM_VAR:
                return Zygosity.HOMOZYGOUS;
            case NO_CALL:
            case UNAVAILABLE:
            default:
                return Zygosity.UNKNOWN;
        }
    }

    public Zygosity zygosity() {
        return zygosity;
    }

    public int dp() {
        return dp;
    }

    public int refReads() {
        return refReads;
    }

    public int altReads() {
        return altReads;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metadata metadata = (Metadata) o;
        return dp == metadata.dp && refReads == metadata.refReads && altReads == metadata.altReads && zygosity == metadata.zygosity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(zygosity, dp, refReads, altReads);
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "zygosity=" + zygosity +
                ", dp=" + dp +
                ", refReads=" + refReads +
                ", altReads=" + altReads +
                '}';
    }
}
