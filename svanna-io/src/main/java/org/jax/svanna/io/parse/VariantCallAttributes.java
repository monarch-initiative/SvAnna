package org.jax.svanna.io.parse;

import org.jax.svanna.core.reference.VariantMetadata;
import org.jax.svanna.core.reference.Zygosity;

import java.util.Objects;

/**
 * POJO for grouping variant data required for implementing {@link VariantMetadata}.
 */
class VariantCallAttributes {

    private final Zygosity zygosity;
    private final int dp;
    private final int refReads;
    private final int altReads;
    private final int copyNumber;

    private VariantCallAttributes(Builder builder) {
        zygosity = Objects.requireNonNull(builder.zygosity);

        if (builder.dp < VariantMetadata.MISSING_DEPTH_PLACEHOLDER) {
            throw new IllegalArgumentException("Minimum depth of coverage must be greater than `-1`: " + builder.dp);
        }
        dp = builder.dp;

        if (builder.refReads < VariantMetadata.MISSING_DEPTH_PLACEHOLDER) {
            throw new IllegalArgumentException("Number of reads supporting ref allele must be greater than `-1`: " + builder.refReads);
        }
        refReads = builder.refReads;

        if (builder.altReads < VariantMetadata.MISSING_DEPTH_PLACEHOLDER) {
            throw new IllegalArgumentException("Number of reads supporting alt allele must be greater than `-1`: " + builder.altReads);
        }
        altReads = builder.altReads;
        if (builder.copyNumber < -1) {
            throw new IllegalArgumentException("Copy number must be greater than `-1`: " + builder.copyNumber);
        }
        copyNumber = builder.copyNumber;
    }

    static Builder builder() {
        return new Builder();
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

    public int copyNumber() {
        return copyNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantCallAttributes that = (VariantCallAttributes) o;
        return dp == that.dp && refReads == that.refReads && altReads == that.altReads && copyNumber == that.copyNumber && zygosity == that.zygosity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(zygosity, dp, refReads, altReads, copyNumber);
    }

    @Override
    public String toString() {
        return "VariantCallAttributes{" +
                "zygosity=" + zygosity +
                ", dp=" + dp +
                ", refReads=" + refReads +
                ", altReads=" + altReads +
                ", copyNumber=" + copyNumber +
                '}';
    }

    static class Builder {

        private Zygosity zygosity = Zygosity.UNKNOWN;
        private int dp = VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        private int refReads = VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        private int altReads = VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        private int copyNumber = -1;

        private Builder() {

        }

        Builder zygosity(Zygosity zygosity) {
            this.zygosity = zygosity;
            return this;
        }

        Builder dp(int dp) {
            this.dp = dp;
            return this;
        }

        Builder refReads(int refReads) {
            this.refReads = refReads;
            return this;
        }

        Builder altReads(int altReads) {
            this.altReads = altReads;
            return this;
        }

        Builder copyNumber(int copyNumber) {
            this.copyNumber = copyNumber;
            return this;
        }

        VariantCallAttributes build() {
            return new VariantCallAttributes(this);
        }

    }
}
