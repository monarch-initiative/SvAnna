package org.jax.svanna.io.parse;

import org.jax.svanna.core.reference.VariantMetadata;
import org.jax.svanna.core.reference.Zygosity;

import java.util.Objects;

/**
 * POJO for grouping variant data required for implementing {@link VariantMetadata}.
 */
class VariantCallAttributes {

    private static final VariantCallAttributes MISSING = new VariantCallAttributes(Zygosity.UNKNOWN,
            VariantMetadata.MISSING_DEPTH_PLACEHOLDER,
            VariantMetadata.MISSING_DEPTH_PLACEHOLDER,
            VariantMetadata.MISSING_DEPTH_PLACEHOLDER);

    static VariantCallAttributes missing() {
        return MISSING;
    }

    private final Zygosity zygosity;
    private final int dp, refReads, altReads;

    VariantCallAttributes(Zygosity zygosity,
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
        VariantCallAttributes variantCallAttributes = (VariantCallAttributes) o;
        return dp == variantCallAttributes.dp && refReads == variantCallAttributes.refReads && altReads == variantCallAttributes.altReads && zygosity == variantCallAttributes.zygosity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(zygosity, dp, refReads, altReads);
    }

    @Override
    public String toString() {
        return "VariantCallAttributes{" +
                "zygosity=" + zygosity +
                ", dp=" + dp +
                ", refReads=" + refReads +
                ", altReads=" + altReads +
                '}';
    }
}
