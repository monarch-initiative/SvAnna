package org.jax.svanna.db.additive.dispatch;

import org.monarchinitiative.svart.Breakend;
import org.monarchinitiative.svart.BreakendVariant;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Variant;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

class InterchromosomalVariantArrangement extends VariantArrangement {

    private final int breakendIndex;
    private final GenomicRegion leftVariantRegion;
    private final GenomicRegion rightVariantRegion;


    InterchromosomalVariantArrangement(List<Variant> variants, int breakendIndex) {
        super(variants);
        this.breakendIndex = breakendIndex;
        BreakendVariant variant = (BreakendVariant) variants.get(breakendIndex);
        this.leftVariantRegion = prepareLeftRegion(variants(), variant.left());
        this.rightVariantRegion = prepareRightRegion(variants(), variant.right());
    }

    static GenomicRegion prepareLeftRegion(LinkedList<Variant> variants, Breakend left) {
        Variant first = variants.getFirst();
        return GenomicRegion.of(left.contig(), left.strand(), CS,
                first.startOnStrandWithCoordinateSystem(left.strand(), CS),
                left.endWithCoordinateSystem(CS));
    }

    static GenomicRegion prepareRightRegion(LinkedList<Variant> variants, Breakend right) {
        GenomicRegion last = variants.getLast();
        if (last instanceof BreakendVariant) {
            // if there is only one variant in `variants`, then `last` should correspond to the right breakend
            last = ((BreakendVariant) last).right();
        }

        return GenomicRegion.of(right.contig(), right.strand(), CS,
                right.startWithCoordinateSystem(CS),
                last.endOnStrandWithCoordinateSystem(right.strand(), CS));
    }

    public BreakendVariant breakendVariant() {
        return (BreakendVariant) variants.get(breakendIndex);
    }

    @Override
    public int breakendIndex() {
        return breakendIndex;
    }

    @Override
    public boolean hasBreakend() {
        return breakendIndex != -1;
    }

    public GenomicRegion leftVariantRegion() {
        return leftVariantRegion;
    }

    public GenomicRegion rightVariantRegion() {
        return rightVariantRegion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        InterchromosomalVariantArrangement that = (InterchromosomalVariantArrangement) o;
        return breakendIndex == that.breakendIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), breakendIndex);
    }


}
