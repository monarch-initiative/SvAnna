package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.overlap.GeneOverlap;
import org.jax.svanna.core.priority.AnnotatedSvPriority;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Deprecated(forRemoval = true)
public class HtmlVisualizable implements Visualizable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlVisualizable.class);

    /**
     * Representation of the structural variant as it came from the VCF file.
     */
    private final SvannaVariant variant;

    private final AnnotatedSvPriority svPriority;

    public HtmlVisualizable(SvannaVariant variant, AnnotatedSvPriority svPriority) {
        this.variant = variant;
        this.svPriority = svPriority;
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
        return Set.copyOf(svPriority.getDiseases());
    }

    /**
     * Count up the number of unique (distinct) genes affected by this structural variant.
     *
     * @return
     */
    @Override
    public int getGeneCount() {
        return (int) this.svPriority.getAffectedTranscripts()
                .stream()
//                .map(Transcript::hgvsSymbol)
                .distinct()
                .count();
    }

    @Override
    public List<Enhancer> enhancers() {
        return this.svPriority.getAffectedEnhancers();
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
        return List.of();
    }

    @Override
    public List<Gene> genes() {
        return List.of();
    }
}
