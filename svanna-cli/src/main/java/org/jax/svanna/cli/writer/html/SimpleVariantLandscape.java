package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.model.landscape.Enhancer;

import java.util.List;
import java.util.Objects;

class SimpleVariantLandscape implements VariantLandscape {

    /**
     * Representation of the structural variant as it came from the VCF file.
     */
    private final SvannaVariant variant;

    private final List<Enhancer> affectedEnhancers;

    private final List<GeneOverlap> overlaps;

    static SimpleVariantLandscape of(SvannaVariant variant, List<GeneOverlap> overlaps, List<Enhancer> affectedEnhancers) {
        return new SimpleVariantLandscape(variant, overlaps, affectedEnhancers);
    }

    protected SimpleVariantLandscape(SvannaVariant variant, List<GeneOverlap> overlaps, List<Enhancer> affectedEnhancers) {
        this.variant = variant;
        this.affectedEnhancers = affectedEnhancers;
        this.overlaps = overlaps;
    }

    @Override
    public SvannaVariant variant() {
        return variant;
    }

    @Override
    public List<GeneOverlap> overlaps() {
        return overlaps;
    }

    @Override
    public List<Enhancer> enhancers() {
        return affectedEnhancers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleVariantLandscape that = (SimpleVariantLandscape) o;
        return Objects.equals(variant, that.variant) && Objects.equals(affectedEnhancers, that.affectedEnhancers) && Objects.equals(overlaps, that.overlaps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, affectedEnhancers, overlaps);
    }

    @Override
    public String toString() {
        return "SimpleVariantLandscape{" +
                "variant=" + variant +
                ", affectedEnhancers=" + affectedEnhancers +
                ", overlaps=" + overlaps +
                '}';
    }
}
