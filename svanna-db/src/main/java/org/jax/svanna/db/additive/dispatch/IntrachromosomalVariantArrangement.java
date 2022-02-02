package org.jax.svanna.db.additive.dispatch;

import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.GenomicVariant;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

class IntrachromosomalVariantArrangement extends VariantArrangement {

    // In intrachromosomal arrangements, it is important to create the variant arrangement on one strand, e.g. POSITIVE
    private static final Strand STRAND = Strand.POSITIVE;
    private final GenomicRegion variantRegion;

    IntrachromosomalVariantArrangement(List<GenomicVariant> variants) {
        super(variants);
        variantRegion = makeIntrachromosomalVariantRegion(variants());
    }

    static GenomicRegion makeIntrachromosomalVariantRegion(LinkedList<GenomicVariant> variants) {
        GenomicVariant first = variants.getFirst();
        GenomicVariant last = variants.getLast();
        return GenomicRegion.of(first.contig(), STRAND, CS,
                first.startOnStrandWithCoordinateSystem(STRAND, CS),
                last.endOnStrandWithCoordinateSystem(STRAND, CS));
    }

    public GenomicRegion variantRegion() {
        return variantRegion;
    }

    @Override
    public int breakendIndex() {
        return -1;
    }

    @Override
    public boolean hasBreakend() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        IntrachromosomalVariantArrangement that = (IntrachromosomalVariantArrangement) o;
        return Objects.equals(variantRegion, that.variantRegion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variantRegion);
    }

    @Override
    public String toString() {
        return "IntrachromosomalVariantArrangement{" +
                "variantRegion=" + variantRegion +
                "} " + super.toString();
    }
}
