package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class SomeVisualizable implements Visualizable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SomeVisualizable.class);

    /**
     * Representation of the structural variant as it came from the VCF file.
     */
    private final SvannaVariant variant;

    private final Set<HpoDiseaseSummary> diseaseSummaries;

    private final List<Enhancer> affectedEnhancers;

    private final List<GeneOverlap> overlaps;

    static SomeVisualizable of(SvannaVariant variant,
                               Set<HpoDiseaseSummary> diseaseSummaries,
                               List<GeneOverlap> overlaps,
                               List<Enhancer> affectedEnhancers) {
        return new SomeVisualizable(variant, diseaseSummaries, overlaps, affectedEnhancers);
    }

    private SomeVisualizable(SvannaVariant variant,
                             Set<HpoDiseaseSummary> diseaseSummaries,
                             List<GeneOverlap> overlaps,
                             List<Enhancer> affectedEnhancers) {
        this.variant = variant;
        this.diseaseSummaries = diseaseSummaries;
        this.overlaps = overlaps;
        this.affectedEnhancers = affectedEnhancers;
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
    public SvannaVariant variant() {
        return this.variant;
    }

    @Override
    public String getType() {
        return variant.variantType().toString();
    }

    @Override
    public Set<HpoDiseaseSummary> diseaseSummaries() {
        return diseaseSummaries;
    }

    @Override
    public List<Gene> genes() {
        return overlaps.stream().map(GeneOverlap::gene).collect(Collectors.toList());
    }

    /**
     * Count up the number of unique (distinct) genes affected by this structural variant.
     *
     * @return
     */
    @Override
    public int getGeneCount() {
        return (int) overlaps.stream()
                .map(GeneOverlap::gene)
                .map(Gene::geneSymbol)
                .distinct()
                .count();
    }

    @Override
    public List<Enhancer> enhancers() {
        return affectedEnhancers;
    }

    /**
     * Return strings for display of the format chr3:123-456
     * We return two strings for translocations.
     *
     * @return
     */
    @Override
    public List<HtmlLocation> locations() {
        List<HtmlLocation> locs = new ArrayList<>();
        switch (variant.variantType().baseType()) {
            case DEL:
            case DUP:
            case INV:
                locs.add(getSimpleLocation(variant));
                break;
            case INS:
                locs.add(getInsertionLocation(variant));
                break;
            case TRA:
            case BND:
                if (variant instanceof BreakendVariant) {
                    locs.addAll(getTranslocationLocations((BreakendVariant) variant));
                }
                break;
            default:
                LOGGER.warn("Unable to get locations for variant type `{}`", variant.variantType());
                break;
        }
        return locs;
    }

    @Override
    public List<GeneOverlap> overlaps() {
        return overlaps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SomeVisualizable that = (SomeVisualizable) o;
        return Objects.equals(variant, that.variant) && Objects.equals(diseaseSummaries, that.diseaseSummaries) && Objects.equals(affectedEnhancers, that.affectedEnhancers) && Objects.equals(overlaps, that.overlaps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variant, diseaseSummaries, affectedEnhancers, overlaps);
    }

    @Override
    public String toString() {
        return "SomeVisualizable{" +
                "variant=" + variant +
                ", diseaseSummaries=" + diseaseSummaries +
                ", affectedEnhancers=" + affectedEnhancers +
                ", overlaps=" + overlaps +
                '}';
    }
}
