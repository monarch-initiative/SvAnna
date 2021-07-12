package org.jax.svanna.core.reference;

import java.util.Objects;

/**
 * POJO for grouping variant data required for implementing {@link VariantMetadata}.
 */
public class VariantCallAttributes {

    private final Zygosity zygosity;
    private final int dp;
    private final int refReads;
    private final int altReads;
    private final int copyNumber;

    private VariantCallAttributes(Builder builder) {
        zygosity = Objects.requireNonNull(builder.zygosity);
        dp = builder.dp;
        refReads = builder.refReads;
        altReads = builder.altReads;

        if (builder.copyNumber < -1) {
            throw new IllegalArgumentException("Copy number must be greater than `-1`: " + builder.copyNumber);
        }
        copyNumber = builder.copyNumber;
    }

    public static Builder builder() {
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

    public static class Builder {

        private Zygosity zygosity = Zygosity.UNKNOWN;
        private int dp = VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        private int refReads = VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        private int altReads = VariantMetadata.MISSING_DEPTH_PLACEHOLDER;
        private int copyNumber = -1;

        private Builder() {}

        public Builder zygosity(Zygosity zygosity) {
            this.zygosity = zygosity;
            return this;
        }

        public Builder dp(int dp) {
            this.dp = dp;
            return this;
        }

        public Builder refReads(int refReads) {
            this.refReads = refReads;
            return this;
        }

        public Builder altReads(int altReads) {
            this.altReads = altReads;
            return this;
        }

        public Builder copyNumber(int copyNumber) {
            this.copyNumber = copyNumber;
            return this;
        }

        public VariantCallAttributes build() {
            return new VariantCallAttributes(this);
        }

    }
}
