package org.jax.svanna.core.viz.svg;


import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.variant.api.Breakend;
import org.monarchinitiative.variant.api.Contig;
import org.monarchinitiative.variant.api.Strand;
import org.monarchinitiative.variant.api.Variant;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.jax.svanna.core.viz.svg.Constants.HEIGHT_PER_DISPLAY_ITEM;
import static org.jax.svanna.core.viz.svg.Constants.SVG_WIDTH;

/**
 * This class is used to create each of the two rows of the final display of the translocation.
 */
public class TranslocationComponentSvgGenerator extends SvSvgGenerator {
    /** Flag to denote a value is not relevant. This may happen for instance if we have no enhancers. */
    private final static int UNINITIALIZED = -42;


    /** This class will display one of the breakpoints of the translocation. This variable stores the position of the
     * breakpoint.
     */
    private final int positionOnContig;
    private final int minTranscriptPos;
    private final int maxTranscriptPos;
    private final int minEnhancerPos;
    private final int maxEnhancerPos;
    private final int minPos;
    private final int maxPos;

    private final Contig contig;

    private final int ystart;

    /**
     * The constructor calculates the left and right boundaries for display
     * TODO document logic, cleanup
     *
     * @param transcripts List of transcripts affected (disrupted) by the translocation
     * @param enhancers List of enhancers affected by the translocation
     */
    public TranslocationComponentSvgGenerator(int minPos, int maxPos,
                                              List<Transcript> transcripts,
                                              List<Enhancer> enhancers,
                                              Variant variant,
                                              Breakend breakend,
                                              int ystart) {
        super(minPos, maxPos, variant, transcripts, enhancers);
        this.contig = breakend.contig();
        this.positionOnContig = breakend.pos();
        this.ystart = ystart;
        this.minTranscriptPos = transcripts.stream().
                map(tx -> tx.withStrand(Strand.POSITIVE)).
                mapToInt(Transcript::start).
                min().orElse(UNINITIALIZED);
        this.maxTranscriptPos = transcripts.stream().
                map(tx -> tx.withStrand(Strand.POSITIVE)).
                mapToInt(Transcript::end).
                max().orElse(UNINITIALIZED);
        this.minEnhancerPos = enhancers.stream()
                .map(e -> e.withStrand(Strand.POSITIVE))
                .mapToInt(Enhancer::start)
                .min().orElse(UNINITIALIZED);
        this.maxEnhancerPos = enhancers.stream()
                .map(e -> e.withStrand(Strand.POSITIVE))
                .mapToInt(Enhancer::end)
                .max().orElse(UNINITIALIZED);
        this.minPos = minPos;
        this.maxPos = maxPos;
    }




    @Override
    public void write(Writer writer) throws IOException {
        writeTranslocation(writer);
    }

    /**
     * Transform a genomic cooordinate to an SVG X coordinate
     *
     * @param genomicCoordinate the position to transform
     * @param genomicMin the minimum genomic position
     * @param genomicMax the maximum genomic position
     * @return SVG position that corresponds to a given genomic position.
     */
    private double translatePositionToSvg(int genomicCoordinate, int genomicMin, int genomicMax) {
        double pos = genomicCoordinate - genomicMin;
        double genomicSpan = genomicMax - genomicMin;
        if (pos < 0) {
            throw new SvAnnRuntimeException("Bad left boundary (genomic coordinate-"); // should never happen
        }
        double prop = pos / genomicSpan;
        return prop * SVG_WIDTH;
    }

    /**
     * Write a zigzag line to indicate the position of the breakpoint
     * @param y1 initial vertical position of the zig-zag line
     * @param n_display_items number of enhancers/transcripts through which the translocation goes
     * @param x horizontal position in SVG coordinates
     * @param writer file handle
     * @throws IOException if we cannot write
     */
    private void writeZigZagLine(int y1, int n_display_items, int x, String description, Writer writer) throws IOException {
        int increment = 3;
        writer.write(String.format("<path d=\"M %d,%d \n", x, y1));
        int y = y1;
        int y2 = y1 + n_display_items*40;
        while (y < y2) {
            writer.write(String.format(" l %d,%d\n", increment, increment));
            writer.write(String.format(" l %d,%d\n", -increment, increment));
            writer.write(String.format(" l %d,%d\n", -increment, increment));
            writer.write(String.format(" l %d,%d\n", increment, increment));
            y += 2 * increment;
        }
        writer.write(" \" style=\"fill: red\" />\n");
        int xpos = (int) getAdjustedXPositionForText(x);
        String text = String.format("<text x=\"%d\" y=\"%d\">%s</text>\n", xpos+20, y1+10, description);
        writer.write(text);
    }


    private int writeTranslocation(Writer writer) throws IOException {
        String chrom = contig.name().startsWith("chr") ? contig.name() : "chr" + contig.name();
        String description = String.format("Translocation breakpoint at %s:%d", chrom, positionOnContig);
        int ypos = this.ystart;
        int offset = 0;
        int n_display_items = 0;
        for (var enh : this.affectedEnhancers) {
            writeEnhancer(enh, ypos, writer);
            ypos += HEIGHT_PER_DISPLAY_ITEM;
            offset += HEIGHT_PER_DISPLAY_ITEM;
            n_display_items++;
        }
        for (var tmod : this.affectedTranscripts) {
            writeTranscript(tmod, ypos, writer);
            ypos += HEIGHT_PER_DISPLAY_ITEM;
            offset += HEIGHT_PER_DISPLAY_ITEM;
            n_display_items++;
        }
        int xpos = (int)translateGenomicToSvg(this.positionOnContig);
        writeZigZagLine(ystart-30,  n_display_items, xpos, description, writer);
        ypos += 50;
        writeScale(writer, this.contig, ypos);
        offset += 50; // for scale
        return offset;
    }
}
