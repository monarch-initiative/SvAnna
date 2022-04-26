package org.monarchinitiative.svanna.cli.writer.html;

import org.monarchinitiative.svanna.core.overlap.GeneOverlap;
import org.monarchinitiative.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svanna.model.HpoDiseaseSummary;
import org.monarchinitiative.svanna.model.landscape.dosage.DosageRegion;
import org.monarchinitiative.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.svanna.model.landscape.repeat.RepetitiveRegion;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class SimpleVisualizable extends SimpleVariantLandscape implements Visualizable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleVisualizable.class);

    private final List<HpoDiseaseSummary> diseaseSummaries;

    private final List<RepetitiveRegion> repetitiveRegions;

    private final List<DosageRegion> dosageRegions;

    static SimpleVisualizable of(SvannaVariant variant,
                                 List<GeneOverlap> overlaps,
                                 List<Enhancer> affectedEnhancers,
                                 List<HpoDiseaseSummary> diseaseSummaries,
                                 List<RepetitiveRegion> repetitiveRegions,
                                 List<DosageRegion> dosageRegions) {
        return new SimpleVisualizable(variant, overlaps, affectedEnhancers, diseaseSummaries, repetitiveRegions, dosageRegions);
    }

    static SimpleVisualizable of(VariantLandscape variantLandscape,
                                 List<HpoDiseaseSummary> diseaseSummaries,
                                 List<RepetitiveRegion> repetitiveRegions,
                                 List<DosageRegion> dosageRegions) {
        return new SimpleVisualizable(variantLandscape.variant(), variantLandscape.overlaps(), variantLandscape.enhancers(), diseaseSummaries, repetitiveRegions, dosageRegions);
    }

    private SimpleVisualizable(SvannaVariant variant,
                               List<GeneOverlap> overlaps,
                               List<Enhancer> affectedEnhancers,
                               List<HpoDiseaseSummary> diseaseSummaries,
                               List<RepetitiveRegion> repetitiveRegions,
                               List<DosageRegion> dosageRegions) {
        super(variant, overlaps, affectedEnhancers, dosageRegions);
        this.diseaseSummaries = diseaseSummaries;
        this.repetitiveRegions = repetitiveRegions;
        this.dosageRegions = dosageRegions;
    }

    private HtmlLocation getSimpleLocation(GenomicVariant variant) {
        // works for INV, DEL, DUP
        return new HtmlLocation(variant.contig(),
                variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()),
                variant.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
    }

    private HtmlLocation getInsertionLocation(GenomicVariant variant) {
        return new HtmlLocation(variant.contig(),
                variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()),
                variant.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
    }

    private List<HtmlLocation> getTranslocationLocations(GenomicBreakendVariant breakended) {
        GenomicBreakend left = breakended.left();
        GenomicBreakend right = breakended.right();
        return List.of(new HtmlLocation(left.contig(),
                        left.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased())),
                new HtmlLocation(right.contig(),
                        right.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased())));
    }

    @Override
    public List<HpoDiseaseSummary> diseaseSummaries() {
        return diseaseSummaries;
    }

    @Override
    public List<RepetitiveRegion> repetitiveRegions() {
        return repetitiveRegions;
    }

    /**
     * Return strings for display of the format chr3:123-456
     * We return two strings for translocations.
     */
    @Override
    public List<HtmlLocation> locations() {
        List<HtmlLocation> locs = new ArrayList<>();
        GenomicVariant genomicVariant = variant().genomicVariant();
        VariantType variantType = genomicVariant.variantType();
        switch (variantType.baseType()) {
            case DEL:
            case DUP:
            case INV:
                locs.add(getSimpleLocation(genomicVariant));
                break;
            case INS:
                locs.add(getInsertionLocation(genomicVariant));
                break;
            case TRA:
            case BND:
                if (genomicVariant instanceof GenomicBreakendVariant)
                    locs.addAll(getTranslocationLocations((GenomicBreakendVariant) genomicVariant));
                else
                    LOGGER.warn("Breakend variant `{}` is not instance of GenomicBreakendVariant", variant().id());
                break;
            default:
                LOGGER.warn("Unable to get locations for variant type `{}`", variantType);
                break;
        }
        return locs;
    }

    @Override
    public List<DosageRegion> dosageRegions() {
        return dosageRegions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SimpleVisualizable that = (SimpleVisualizable) o;
        return Objects.equals(diseaseSummaries, that.diseaseSummaries) && Objects.equals(repetitiveRegions, that.repetitiveRegions) && Objects.equals(dosageRegions, that.dosageRegions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), diseaseSummaries, repetitiveRegions, dosageRegions);
    }

    @Override
    public String toString() {
        return "SimpleVisualizable{" +
                "diseaseSummaries=" + diseaseSummaries +
                ", repetitiveRegions=" + repetitiveRegions +
                ", dosageRegions=" + dosageRegions +
                "} " + super.toString();
    }
}
