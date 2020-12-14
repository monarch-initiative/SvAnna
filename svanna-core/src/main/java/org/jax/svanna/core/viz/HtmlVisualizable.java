package org.jax.svanna.core.viz;

import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.prioritizer.SvPriority;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.variant.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HtmlVisualizable implements Visualizable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HtmlVisualizable.class);

    /**
     * Representation of the structural variant as it came from the VCF file.
     */
    private final SvannaVariant variant;

    private final SvPriority svPriority;

    public HtmlVisualizable(SvannaVariant variant, SvPriority svPriority) {
        this.variant = variant.withCoordinateSystem(CoordinateSystem.ZERO_BASED);
        this.svPriority = svPriority;
    }

    private HtmlLocation getSimpleLocation(Variant deletion) {
        // works for INV, DEL, DUP
        return new HtmlLocation(deletion.contig(), deletion.start(), deletion.end());
    }

    private HtmlLocation getInsertionLocation(Variant variant) {
        GenomicRegion onPositive = variant.withStrand(Strand.POSITIVE);
        return new HtmlLocation(onPositive.contig(), onPositive.start(), onPositive.end());
    }

    private List<HtmlLocation> getTranslocationLocations(Breakended breakended) {
        Breakend left = breakended.left().withStrand(Strand.POSITIVE);
        Breakend right = breakended.right().withStrand(Strand.POSITIVE);
        return List.of(new HtmlLocation(left.contig(), left.pos()),
                new HtmlLocation(right.contig(), right.pos()));
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
    public String getImpact() {
        return svPriority.getImpact().toString();
    }

    @Override
    public boolean hasPhenotypicRelevance() {
        return this.svPriority.hasPhenotypicRelevance();
    }

    @Override
    public List<HpoDiseaseSummary> diseaseSummaries() {
        return this.svPriority.getDiseases();
    }

    @Override
    public List<Transcript> transcripts() {
        return new ArrayList<>(this.svPriority.getAffectedTranscripts());
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
                .map(Transcript::hgvsSymbol)
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
                if (variant instanceof Breakended) {
                    locs.addAll(getTranslocationLocations((Breakended) variant));
                }
                break;
            default:
                LOGGER.warn("Unable to get locations for variant type `{}`", variant.variantType());
                break;
        }
        return locs;
    }

    @Override
    public List<Overlap> overlaps() {
        return svPriority.getOverlaps();
    }
}
