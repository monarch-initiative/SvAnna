package org.jax.svann.viz.svg;

import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.transcripts.SvAnnTxModel;

import java.io.IOException;
import java.io.Writer;
import java.util.List;


/**
 * This class is used to create each of the two rows of the final display of the translocation.
 */
public class TranslocationComponentSvgGenerator extends  SvSvgGenerator {
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
     * @param transcripts
     * @param enhancers       // * @param genomeInterval
     * @param coordinatePairs
     */
    public TranslocationComponentSvgGenerator(int minPos, int maxPos,
                                              List<SvAnnTxModel> transcripts,
                                              List<Enhancer> enhancers,
                                              List<CoordinatePair> coordinatePairs,
                                              Contig contig,
                                              int contigPos,
                                              int ystart) {
        super(minPos, maxPos, SvType.TRANSLOCATION, transcripts, enhancers, coordinatePairs);
        this.contig = contig;
        this.positionOnContig = contigPos;
        this.ystart = ystart;
        this.minTranscriptPos = transcripts.stream().
                map(tx -> tx.withStrand(Strand.FWD)).
                mapToInt(SvAnnTxModel::getStartPosition).
                min().orElse(UNINITIALIZED);
        this.maxTranscriptPos = transcripts.stream().
                map(tx -> tx.withStrand(Strand.FWD)).
                mapToInt(SvAnnTxModel::getEndPosition).
                max().orElse(UNINITIALIZED);
        this.minEnhancerPos = enhancers.stream()
                .map(Enhancer::getStart)
                .mapToInt(GenomicPosition::getPosition)
                .min().orElse(UNINITIALIZED);
        this.maxEnhancerPos = enhancers.stream()
                .map(Enhancer::getEnd)
                .mapToInt(GenomicPosition::getPosition)
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
     * @return
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
     * @param y1
     * @param n_display_items number of enhancers/transcripts through which the translocation goes
     * @param x
     * @param writer
     * @throws IOException
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
        String text = String.format("<text x=\"%d\" y=\"%d\">%s</text>\n", x+20, y1+10, description);
        writer.write(text);
    }


    private int writeTranslocation(Writer writer) throws IOException {
        String description = String.format("Translocation breakpoint at %s:%d", contig.getPrimaryName(), positionOnContig);
        int ypos = this.ystart;
        int offset = 0;
        int n_display_items = 0;
        for (var enh : this.affectedEnhancers) {
            // writeEnhancer(enh, ypos, writer);
            System.err.println("[ERR] Warning not implemented yet");
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
