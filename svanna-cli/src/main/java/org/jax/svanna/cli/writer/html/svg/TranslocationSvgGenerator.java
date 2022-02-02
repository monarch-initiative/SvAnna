package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.repeat.RepetitiveRegion;
import org.monarchinitiative.svart.*;
import xyz.ielis.silent.genes.model.Gene;
import xyz.ielis.silent.genes.model.Spliced;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

/**
 * This class creates an SVG picture that illustrates the effects of a translocation. It shows each breakpoint separately
 * i.e., it shows two rows, one for each affected Contig. It does not attempt to show the resulting derived chromosomes.
 */
public class TranslocationSvgGenerator extends SvSvgGenerator {

    private static final CoordinateSystem COORDINATE_SYSTEM = CoordinateSystem.oneBased();

    private final static int YSTART = 50;

    private final int separationLineY;
    private final int ystartB;

    private final TranslocationComponentSvgGenerator componentA;
    private final TranslocationComponentSvgGenerator componentB;


    /**
     * The constructor calculates the left and right boundaries for display.
     * @param variant Variant object representing a translocation
     * @param breakended
     * @param genes genes affected by this translocation
     * @param enhancers Enhancers disrupted by this translocation
     */
    public TranslocationSvgGenerator(GenomicVariant variant,
                                     GenomicBreakendVariant breakended,
                                     List<Gene> genes,
                                     List<Enhancer> enhancers,
                                     List<RepetitiveRegion> repeats) {
        super(variant, genes, enhancers, repeats, List.of());
        GenomicBreakend left = breakended.left();
        GenomicBreakend right = breakended.right();

        List<Gene> genesA = genes.stream().filter(t -> t.contigId() == left.contigId()).collect(Collectors.toList());
        List<Gene> genesB = genes.stream().filter(t -> t.contigId() == right.contigId()).collect(Collectors.toList());

        List<Enhancer> enhancersA = enhancers.stream().filter(e -> e.contigId() == left.contigId()).collect(Collectors.toList());
        List<Enhancer> enhancersB = enhancers.stream().filter(e -> e.contigId() == right.contigId()).collect(Collectors.toList());

        List<RepetitiveRegion> repeatsA = repeats.stream().filter(r -> r.contigId() == left.contigId()).collect(Collectors.toList());
        List<RepetitiveRegion> repeatsB = repeats.stream().filter(r -> r.contigId() == right.contigId()).collect(Collectors.toList());

        int nTranscripts = genesA.stream()
                .mapToInt(Spliced::transcriptCount)
                .sum();
        int displayElementsA = nTranscripts + enhancersA.size(); // number of display elements for translocation A

        this.separationLineY = displayElementsA*Constants.HEIGHT_PER_DISPLAY_ITEM + YSTART + 100;
        this.ystartB = this.separationLineY + 50;

        this.componentA = new TranslocationComponentSvgGenerator(
                getMin(genesA, enhancersA, left.startOnStrandWithCoordinateSystem(Strand.POSITIVE, COORDINATE_SYSTEM)),
                getMax(genesA, enhancersA, left.endOnStrandWithCoordinateSystem(Strand.POSITIVE, COORDINATE_SYSTEM), left.contig().length()),
                genesA, enhancersA, repeatsA, variant, left, YSTART);
        this.componentB = new TranslocationComponentSvgGenerator(
                getMin(genesB, enhancersB, right.startOnStrandWithCoordinateSystem(Strand.POSITIVE, COORDINATE_SYSTEM)),
                getMax(genesB, enhancersB, right.endOnStrandWithCoordinateSystem(Strand.POSITIVE, COORDINATE_SYSTEM), right.contig().length()),
                genesB, enhancersB, repeatsB, variant, right, ystartB);
    }

    static int getMin(List<Gene> genes, List<Enhancer> enhancers, int contigPos) {
        contigPos = Math.max(1, contigPos - 1000); // add 1000 bp padding
        OptionalInt minGenePos = genes.stream().
                mapToInt(tx -> tx.startOnStrand(Strand.POSITIVE)).
                min();
        OptionalInt minEnhancerPos = enhancers.stream()
                .mapToInt(e -> e.startOnStrand(Strand.POSITIVE))
                .min();

        if (minGenePos.isEmpty() && minEnhancerPos.isEmpty()) {
            return contigPos;
        } else if (minEnhancerPos.isEmpty()) {
            return Math.min(minGenePos.getAsInt(), contigPos);
        } else if (minGenePos.isEmpty()) {
            return Math.min(minEnhancerPos.getAsInt(), contigPos);
        } else {
            return Math.min(contigPos, Math.min(minEnhancerPos.getAsInt(), minGenePos.getAsInt()));
        }
    }

   private static int getMax(List<Gene> genes, List<Enhancer> enhancers, int contigPos, int contigLength) {
        contigPos =  Math.min(contigPos + 1000, contigLength); // add 1000 bp padding
        OptionalInt maxTranscriptPos = genes.stream().
                mapToInt(g -> g.endOnStrand(Strand.POSITIVE)).
                max();

        OptionalInt maxEnhancerPos = enhancers.stream()
                .mapToInt(e -> e.endOnStrand(Strand.POSITIVE))
                .max();
        if (maxTranscriptPos.isEmpty() && maxEnhancerPos.isEmpty()) {
            return contigPos;
        } else if (maxEnhancerPos.isEmpty()) {
            return Math.max(maxTranscriptPos.getAsInt(), contigPos);
        } else if (maxTranscriptPos.isEmpty()) {
            return Math.max(maxEnhancerPos.getAsInt(), contigPos);
        } else {
            return Math.max(contigPos, Math.min(maxEnhancerPos.getAsInt(), maxTranscriptPos.getAsInt()));
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
