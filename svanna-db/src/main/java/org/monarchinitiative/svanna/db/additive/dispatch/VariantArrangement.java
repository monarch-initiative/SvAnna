package org.monarchinitiative.svanna.db.additive.dispatch;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A list of variants arranged in an order that allows creating the annotation route. The variant list contains max 1
 * breakend variant, location of the breakend variant is indicated by the {@link #breakendIndex}.
 */
abstract class VariantArrangement {

    protected static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    protected final LinkedList<GenomicVariant> variants;

    protected VariantArrangement(List<GenomicVariant> variants) {
        this.variants = (variants instanceof LinkedList)
                ? (LinkedList<GenomicVariant>) variants
                : new LinkedList<>(variants);
    }

    static IntrachromosomalVariantArrangement intrachromosomal(List<GenomicVariant> variants) {
        return new IntrachromosomalVariantArrangement(variants);
    }

    static VariantArrangement interchromosomal(List<GenomicVariant> variants, int breakendIdx) {
        return new InterchromosomalVariantArrangement(variants, breakendIdx);
    }

    public LinkedList<GenomicVariant> variants() {
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
