package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.core.SvAnnaRuntimeException;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.RepetitiveRegion;
import org.jax.svanna.core.reference.Gene;
import org.monarchinitiative.svart.*;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class creates an SVG picture that illustrates the effects of a translocation. It shows each breakpoint separately
 * i.e., it shows two rows, one for each affected Contig. It does not attempt to show the resulting derived chromosomes.
 */
public class TranslocationSvgGenerator extends SvSvgGenerator {


    private final static int YSTART = 50;
    private final int separationLineY;
    private final int ystartB;

    /**
     * transcripts on contig A
     */
    private final List<Gene> genesA;
    /**
     * transcripts on contig B
     */
    private final List<Gene> genesB;

    /**
     * enhancers on contig A
     */
    private final List<Enhancer> enhancersA;
    /**
     * enhancers on contig B
     */
    private final List<Enhancer> enhancersB;

    private final List<RepetitiveRegion> repeatsA;
    private final List<RepetitiveRegion> repeatsB;

    /**
     * Flag to denote a value is not relevant. This may happen for instance if we have no enhancers.
     */
    private final static int UNINITIALIZED = -42;


    private final TranslocationComponentSvgGenerator componentA;
    private final TranslocationComponentSvgGenerator componentB;


    /**
     * The constructor calculates the left and right boundaries for display.
     * @param variant Variant object representing a translocation
     * @param breakended
     * @param genes genes affected by this translocation
     * @param enhancers Enhancers disrupted by this translocation
     */
    public TranslocationSvgGenerator(Variant variant,
                                     BreakendVariant breakended,
                                     List<Gene> genes,
                                     List<Enhancer> enhancers,
                                     List<RepetitiveRegion> repeats) {
        super(variant, genes, enhancers, repeats);
        Breakend left = breakended.left();
        Breakend right = breakended.right();
        this.genesA = genes.stream().filter(t -> t.contigId() == left.contigId()).collect(Collectors.toList());
        this.genesB = genes.stream().filter(t -> t.contigId() == right.contigId()).collect(Collectors.toList());
        this.enhancersA = enhancers.stream().filter(e -> e.contigId() == left.contigId()).collect(Collectors.toList());
        this.enhancersB = enhancers.stream().filter(e -> e.contigId() == right.contigId()).collect(Collectors.toList());
        this.repeatsA = repeats.stream().filter(r -> r.contigId() == left.contigId()).collect(Collectors.toList());
        this.repeatsB = repeats.stream().filter(r -> r.contigId() == right.contigId()).collect(Collectors.toList());
        int nTranscripts = genesA.stream()
                .mapToInt(g -> g.codingTranscripts().size() + g.nonCodingTranscripts().size())
                .sum();
        int displayElementsA = nTranscripts + enhancersA.size(); // number of display elements for translocation A
        this.separationLineY = displayElementsA*Constants.HEIGHT_PER_DISPLAY_ITEM + YSTART + 100;
        this.ystartB = this.separationLineY + 50;

        CoordinateSystem cs = CoordinateSystem.oneBased();
        this.componentA = new TranslocationComponentSvgGenerator(
                getMin(genesA, enhancersA, left.startWithCoordinateSystem(cs)),
                getMax(genesA, enhancersA, left.endWithCoordinateSystem(cs)),
                genesA, enhancersA, repeatsA, variant, left, YSTART);
        this.componentB = new TranslocationComponentSvgGenerator(
                getMin(genesB, enhancersB, right.startWithCoordinateSystem(cs)),
                getMax(genesB, enhancersB, right.endWithCoordinateSystem(cs)),
                genesB, enhancersB, repeatsB, variant, right, ystartB);
    }

    private static int getMin(List<Gene> genes, List<Enhancer> enhancers, int contigPos) {
        contigPos = Math.max(0, contigPos - 1000); // add 1000 bp padding
        int minGenePos = genes.stream().
                mapToInt(tx -> tx.startOnStrand(Strand.POSITIVE)).
                min().orElse(UNINITIALIZED);
        int minEnhancerPos = enhancers.stream()
                .mapToInt(e -> e.startOnStrand(Strand.POSITIVE))
                .min().orElse(UNINITIALIZED);
        if (minGenePos == UNINITIALIZED && minEnhancerPos == UNINITIALIZED) {
            throw new SvAnnaRuntimeException("Cannot draw translocation with no transcripts and no enhancers!");
        } else if (minEnhancerPos == UNINITIALIZED) {
            return Math.min(minGenePos, contigPos);
        } else if (minGenePos == UNINITIALIZED) {
            return Math.min(minEnhancerPos, contigPos);
        } else {
            return Math.min(contigPos, Math.min(minEnhancerPos, minGenePos));
        }
    }

    private static int getMax(List<Gene> genes, List<Enhancer> enhancers, int contigPos) {
        contigPos =  contigPos + 1000; // add 1000 bp padding
        int maxTranscriptPos = genes.stream().
                mapToInt(g -> g.endOnStrand(Strand.POSITIVE)).
                max().orElse(UNINITIALIZED);
        int maxEnhancerPos = enhancers.stream()
                .mapToInt(e -> e.endOnStrand(Strand.POSITIVE))
                .max().orElse(UNINITIALIZED);
        if (maxTranscriptPos == UNINITIALIZED && maxEnhancerPos == UNINITIALIZED) {
            throw new SvAnnaRuntimeException("Cannot draw translocation with no transcripts and no enhancers!");
        } else if (maxEnhancerPos == UNINITIALIZED) {
            return Math.max(maxTranscriptPos, contigPos);
        } else if (maxTranscriptPos == UNINITIALIZED) {
            return Math.max(maxEnhancerPos, contigPos);
        } else {
            return Math.max(contigPos, Math.min(maxEnhancerPos, maxTranscriptPos));
        }
    }


    private void writeSeparationLine(int ypos, Writer writer) throws IOException {
        int x1 = 20;
        int x2 = Constants.SVG_WIDTH - 20;
        String line = String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" style=\"stroke:rgb(192,192,192);stroke-width:1 stroke-dasharray: 5 2\" />",
                x1, ypos, x2, ypos);
        writer.write(line);
    }


    public void write(Writer writer) throws IOException {
        componentA.write(writer);
        writeSeparationLine(this.separationLineY, writer);
        componentB.write(writer);
    }
}
