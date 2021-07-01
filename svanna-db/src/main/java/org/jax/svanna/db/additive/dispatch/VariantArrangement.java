package org.jax.svanna.db.additive.dispatch;

import org.monarchinitiative.svart.Variant;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A list of variants arranged in an order that allows creating the annotation route. The variant list contains max 1
 * breakend variant, location of the breakend variant is indicated by the {@link #breakendIndex}.
 *
 */
class VariantArrangement {

    private final LinkedList<Variant> variants;
    private final int breakendIndex;

    private VariantArrangement(List<Variant> variants, int breakendIndex) {
        this.variants = new LinkedList<>(variants);
        this.breakendIndex = breakendIndex;
    }

    public LinkedList<Variant> variants() {
        return variants;
    }

    public int size() {
        return variants.size();
    }

    public int breakendIndex() {
        return breakendIndex;
    }

    public boolean hasBreakend() {
        return breakendIndex >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantArrangement that = (VariantArrangement) o;
        return breakendIndex == that.breakendIndex && Objects.equals(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variants, breakendIndex);
    }

    static VariantArrangement intrachromosomal(List<Variant> variants) {
        return new VariantArrangement(variants, -1);
    }

    static VariantArrangement interchromosomal(List<Variant> variants, int breakendIdx) {
        return new VariantArrangement(variants, breakendIdx);
    }
}
