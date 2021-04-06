package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.RepetitiveRegion;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class SimpleVisualizable extends SimpleVariantLandscape implements Visualizable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleVisualizable.class);

    private final Set<HpoDiseaseSummary> diseaseSummaries;

    private final List<RepetitiveRegion> repetitiveRegions;

    static SimpleVisualizable of(SvannaVariant variant,
                                 List<GeneOverlap> overlaps,
                                 List<Enhancer> affectedEnhancers,
                                 Set<HpoDiseaseSummary> diseaseSummaries,
                                 List<RepetitiveRegion> repetitiveRegions) {
        return new SimpleVisualizable(variant, overlaps, affectedEnhancers, diseaseSummaries, repetitiveRegions);
    }

    static SimpleVisualizable of(VariantLandscape variantLandscape, Set<HpoDiseaseSummary> diseaseSummaries, List<RepetitiveRegion> repetitiveRegions) {
        return new SimpleVisualizable(variantLandscape.variant(), variantLandscape.overlaps(), variantLandscape.enhancers(), diseaseSummaries, repetitiveRegions);
    }

    private SimpleVisualizable(SvannaVariant variant,
                               List<GeneOverlap> overlaps,
                               List<Enhancer> affectedEnhancers,
                               Set<HpoDiseaseSummary> diseaseSummaries,
                               List<RepetitiveRegion> repetitiveRegions) {
        super(variant, overlaps, affectedEnhancers);
        this.diseaseSummaries = diseaseSummaries;
        this.repetitiveRegions = repetitiveRegions;
    }

    private HtmlLocation getSimpleLocation(Variant variant) {
        // works for INV, DEL, DUP
        return new HtmlLocation(variant.contig(),
                variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()),
                variant.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
    }

    private HtmlLocation getInsertionLocation(Variant variant) {
        return new HtmlLocation(variant.contig(),
                variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()),
                variant.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
    }

    private List<HtmlLocation> getTranslocationLocations(BreakendVariant breakended) {
        Breakend left = breakended.left();
        Breakend right = breakended.right();
        return List.of(new HtmlLocation(left.contig(),
                        left.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased())),
                new HtmlLocation(right.contig(),
                        right.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased())));
    }

    @Override
    public Set<HpoDiseaseSummary> diseaseSummaries() {
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
        switch (variant().variantType().baseType()) {
            case DEL:
            case DUP:
            case INV:
                locs.add(getSimpleLocation(variant()));
                break;
            case INS:
                locs.add(getInsertionLocation(variant()));
                break;
            case TRA:
            case BND:
                if (variant() instanceof BreakendVariant) {
                    locs.addAll(getTranslocationLocations((BreakendVariant) variant()));
                }
                break;
            default:
                LOGGER.warn("Unable to get locations for variant type `{}`", variant().variantType());
                break;
        }
        return locs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SimpleVisualizable that = (SimpleVisualizable) o;
        return Objects.equals(diseaseSummaries, that.diseaseSummaries) && Objects.equals(repetitiveRegions, that.repetitiveRegions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), diseaseSummaries, repetitiveRegions);
    }

    @Override
    public String toString() {
        return "SimpleVisualizable{" +
                "diseaseSummaries=" + diseaseSummaries +
                ", repetitiveRegions=" + repetitiveRegions +
                '}';
    }
}
