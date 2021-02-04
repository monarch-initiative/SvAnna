package org.jax.svanna.core.viz.svg;

import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.*;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

import static org.jax.svanna.core.viz.svg.Constants.HEIGHT_PER_DISPLAY_ITEM;

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
    private final List<Transcript> transcriptModelsA;
    /**
     * transcripts on contig B
     */
    private final List<Transcript> transcriptModelsB;

    /**
     * enhancers on contig A
     */
    private final List<Enhancer> enhancersA;
    /**
     * enhancers on contig B
     */
    private final List<Enhancer> enhancersB;
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
     * @param transcripts transcripts affected by this translocation
     * @param enhancers Enhancers disrupted by this translocation
     */
    public TranslocationSvgGenerator(Variant variant,
                                     BreakendVariant breakended,
                                     List<Transcript> transcripts,
                                     List<Enhancer> enhancers) {
        super(variant, transcripts, enhancers);
        Breakend left = breakended.left();
        Breakend right = breakended.right();
        this.transcriptModelsA = transcripts.stream().filter(t -> t.contigId() == left.contigId()).collect(Collectors.toList());
        this.transcriptModelsB = transcripts.stream().filter(t -> t.contigId() == right.contigId()).collect(Collectors.toList());
        this.enhancersA = enhancers.stream().filter(e -> e.contigId() == left.contigId()).collect(Collectors.toList());
        this.enhancersB = enhancers.stream().filter(e -> e.contigId() == right.contigId()).collect(Collectors.toList());
        int displayElementsA = transcriptModelsA.size() + enhancersA.size(); // number of display elements for translocation A
        this.separationLineY = displayElementsA*HEIGHT_PER_DISPLAY_ITEM + YSTART + 100;
        this.ystartB = this.separationLineY + 50;

        CoordinateSystem cs = CoordinateSystem.oneBased();
        this.componentA = new TranslocationComponentSvgGenerator(
                getMin(transcriptModelsA, enhancersA, left.startWithCoordinateSystem(cs)),
                getMax(transcriptModelsA, enhancersA, left.endWithCoordinateSystem(cs)),
                transcriptModelsA, enhancersA, variant, left, YSTART);
        this.componentB = new TranslocationComponentSvgGenerator(
                getMin(transcriptModelsB, enhancersB, right.startWithCoordinateSystem(cs)),
                getMax(transcriptModelsB, enhancersB, right.endWithCoordinateSystem(cs)),
                transcriptModelsB, enhancersB, variant, right, ystartB);
    }

    private int getMin(List<Transcript> transcripts, List<Enhancer> enhancers, int contigPos) {
        contigPos = Math.max(0, contigPos - 1000); // add 1000 bp padding
        int minTranscriptPos = transcripts.stream().
                map(tx -> tx.withStrand(Strand.POSITIVE)).
                mapToInt(Transcript::start).
                min().orElse(UNINITIALIZED);
        int minEnhancerPos = enhancers.stream()
                .map(e -> e.withStrand(Strand.POSITIVE))
                .mapToInt(Enhancer::start)
                .min().orElse(UNINITIALIZED);
        if (minTranscriptPos == UNINITIALIZED && minEnhancerPos == UNINITIALIZED) {
            throw new SvAnnRuntimeException("Cannot draw translocation with no transcripts and no enhancers!");
        } else if (minEnhancerPos == UNINITIALIZED) {
            return Math.min(minTranscriptPos, contigPos);
        } else if (minTranscriptPos == UNINITIALIZED) {
            return Math.min(minEnhancerPos, contigPos);
        } else {
            return Math.min(contigPos, Math.min(minEnhancerPos, minTranscriptPos));
        }
    }

    private int getMax(List<Transcript> transcripts, List<Enhancer> enhancers, int contigPos) {
        contigPos =  contigPos + 1000; // add 1000 bp padding
        int maxTranscriptPos = transcripts.stream().
                map(tx -> tx.withStrand(Strand.POSITIVE)).
                mapToInt(Transcript::end).
                max().orElse(UNINITIALIZED);
        int maxEnhancerPos = enhancers.stream()
                .map(e -> e.withStrand(Strand.POSITIVE))
                .mapToInt(Enhancer::end)
                .max().orElse(UNINITIALIZED);
        if (maxTranscriptPos == UNINITIALIZED && maxEnhancerPos == UNINITIALIZED) {
            throw new SvAnnRuntimeException("Cannot draw translocation with no transcripts and no enhancers!");
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
