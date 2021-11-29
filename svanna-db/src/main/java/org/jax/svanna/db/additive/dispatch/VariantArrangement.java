package org.jax.svanna.db.additive.dispatch;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Variant;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A list of variants arranged in an order that allows creating the annotation route. The variant list contains max 1
 * breakend variant, location of the breakend variant is indicated by the {@link #breakendIndex}.
 */
abstract class VariantArrangement {

    protected static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    protected final LinkedList<Variant> variants;

    protected VariantArrangement(List<Variant> variants) {
        this.variants = (variants instanceof LinkedList)
                ? (LinkedList<Variant>) variants
                : new LinkedList<>(variants);
    }

    static IntrachromosomalVariantArrangement intrachromosomal(List<Variant> variants) {
        return new IntrachromosomalVariantArrangement(variants);
    }

    static VariantArrangement interchromosomal(List<Variant> variants, int breakendIdx) {
        return new InterchromosomalVariantArrangement(variants, breakendIdx);
    }

    public LinkedList<Variant> variants() {
        return variants;
    }

    public int size() {
        return variants.size();
    }

    public abstract int breakendIndex();

    public abstract boolean hasBreakend();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariantArrangement that = (VariantArrangement) o;
        return Objects.equals(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variants);
    }

    @Override
    public String toString() {
        return "VariantArrangement{" +
                "variants=" + variants +
                '}';
    }
}
