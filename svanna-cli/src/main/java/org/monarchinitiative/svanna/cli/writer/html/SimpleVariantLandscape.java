package org.monarchinitiative.svanna.cli.writer.html;

import org.monarchinitiative.svanna.core.overlap.GeneOverlap;
import org.monarchinitiative.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svanna.model.landscape.dosage.DosageRegion;
import org.monarchinitiative.svanna.model.landscape.enhancer.Enhancer;

import java.util.List;
import java.util.Objects;

class SimpleVariantLandscape implements VariantLandscape {

    /**
     * Representation of the structural variant as it came from the VCF file.
     */
    private final SvannaVariant variant;

    private final List<Enhancer> enhancers;

    private final List<GeneOverlap> overlaps;

    private final List<DosageRegion> dosageRegions;

    static SimpleVariantLandscape of(SvannaVariant variant,
                                     List<GeneOverlap> overlaps,
                                     List<Enhancer> enhancers,
                                     List<DosageRegion> dosageRegions) {
        return new SimpleVariantLandscape(variant, overlaps, enhancers, dosageRegions);
    }

    protected SimpleVariantLandscape(SvannaVariant variant,
                                     List<GeneOverlap> overlaps,
                                     List<Enhancer> enhancers,
                                     List<DosageRegion> dosageRegions) {
        this.variant = variant;
        this.enhancers = enhancers;
        this.overlaps = overlaps;
        this.dosageRegions = dosageRegions;
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
        return enhancers;
    }

    @Override
    public List<DosageRegion> dosageRegions() {
        return dosageRegions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleVariantLandscape that = (SimpleVariantLandscape) o;
        return Objects.equals(variant, that.variant) && Objects.equals(enhancers, that.enhancers) && Objects.equals(overlaps, that.overlaps) && Objects.equals(dosageRegions, that.dosageRegions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, enhancers, overlaps, dosageRegions);
    }

    @Override
    public String toString() {
        return "SimpleVariantLandscape{" +
                "variant=" + variant +
                ", enhancers=" + enhancers +
                ", overlaps=" + overlaps +
                ", dosageRegions=" + dosageRegions +
                '}';
    }
}
