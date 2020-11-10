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
    public TranslocationComponentSvgGenerator(List<SvAnnTxModel> transcripts,
                                              List<Enhancer> enhancers,
                                              List<CoordinatePair> coordinatePairs,
                                              Contig contig,
                                              int contigPos,
                                              int ystart) {
        super(SvType.TRANSLOCATION, transcripts, enhancers, coordinatePairs);
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
        int min = getMinimumInitializedValue(this.minTranscriptPos, this.minEnhancerPos);
        if (this.positionOnContig < min) {
            this.minPos = this.positionOnContig - 1000;
        } else {
            this.minPos = min;
        }
        int max = getMaximumInitializedValue(this.maxEnhancerPos, this.maxTranscriptPos);
        if (max < this.positionOnContig) {
            this.maxPos = this.positionOnContig + 1000;
        } else {
            this.maxPos = max;
        }
    }


    private int getMinimumInitializedValue(int val1, int val2) {
        if (val1 == UNINITIALIZED && val2 == UNINITIALIZED) {
            throw new SvAnnRuntimeException("Cannot draw translocation with no transcripts and no enhancers!");
        } else if (val1 == UNINITIALIZED) {
            return val2;
        } else if (val2 == UNINITIALIZED) {
            return val1;
        } else {
            return Math.min(val1, val2);
        }
    }
    private int getMaximumInitializedValue(int val1, int val2) {
        if (val1 == UNINITIALIZED && val2 == UNINITIALIZED) {
            throw new SvAnnRuntimeException("Cannot draw translocation with no transcripts and no enhancers!");
        } else if (val1 == UNINITIALIZED) {
            return val2;
        } else if (val2 == UNINITIALIZED) {
            return val1;
        } else {
            return Math.max(val1, val2);
        }
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
     * @param y2
     * @param x
     * @param writer
     * @throws IOException
     */
    private void writeZigZagLine(int y1, int y2, int x, String description, Writer writer) throws IOException {
        int increment = 3;
        writer.write(String.format("<path d=\"M %d,%d \n", x, y1));
        int y = y1;
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
        for (var enh : this.affectedEnhancers) {
            // writeEnhancer(enh, ypos, writer);
            System.err.println("[ERR] Warning not implemented yet");
            ypos += HEIGHT_PER_DISPLAY_ITEM;
            offset += HEIGHT_PER_DISPLAY_ITEM;
        }
        for (var tmod : this.affectedTranscripts) {
            writeTranscript(tmod, ypos, writer);
            ypos += HEIGHT_PER_DISPLAY_ITEM;
            offset += HEIGHT_PER_DISPLAY_ITEM;
        }
        int yend = ystart + offset - (int)(1.5*HEIGHT_PER_DISPLAY_ITEM);
        int xpos = (int)translatePositionToSvg(this.positionOnContig, this.minPos, this.maxPos);
        writeZigZagLine(ystart-30,  yend, xpos, description, writer);
        ypos += 50;
        writeScale(writer, this.contig, ypos);
        offset += 50; // for scale
        return offset;
    }
}
