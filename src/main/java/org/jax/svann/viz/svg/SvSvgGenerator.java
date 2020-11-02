package org.jax.svann.viz.svg;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;

import java.awt.desktop.OpenFilesEvent;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Structural variant (SV) Scalar Vector Graphic (SVG) generator.
 */
public class SvSvgGenerator {

    /** Canvas width of the SVG */
    private final int SVG_WIDTH = 1400;

    private final static int HEIGHT_FOR_SV_DISPLAY = 200;
    private final static int HEIGHT_PER_DISPLAY_ITEM = 100;
    /** Canvas height of the SVG.*/
    private final int SVG_HEIGHT;

    private final List<TranscriptModel> affectedTranscripts;
    private final List<Enhancer> affectedEnhancers;
    private final GenomeInterval svInterval;


    private final int span;
    private final int offset;

    private final int leftBoundary;
    private final int rightBoundary;
    private final String chrom;

    /**
     * The constructor calculates the left and right boundaries for display
     * TODO document logic
     * @param transcripts
     * @param enhancers
     * @param genomeInterval
     */
    public SvSvgGenerator(List<TranscriptModel> transcripts,
                          List<Enhancer> enhancers,
                          GenomeInterval genomeInterval) {
        this.affectedTranscripts = transcripts;
        this.affectedEnhancers = enhancers;
        this.svInterval = genomeInterval;
        int MIN = Integer.MAX_VALUE;
        int MAX = Integer.MIN_VALUE;
        for (var tmod : affectedTranscripts) {
            GenomeInterval gi = tmod.getTXRegion().withStrand(Strand.FWD);
            if (gi.getBeginPos() < MIN)
                MIN = gi.getBeginPos();
            if (gi.getEndPos() > MAX)
                MAX = gi.getEndPos();
        }
        for (var enh : affectedEnhancers) {
            if (enh.getStart().getPos() < MIN)
                MIN = enh.getStart().getPos();
            if (enh.getEnd().getPos() > MAX)
                MAX = enh.getEnd().getPos();
        }
        if (this.svInterval.withStrand(Strand.FWD).getBeginPos() < MIN)
            MIN = this.svInterval.withStrand(Strand.FWD).getBeginPos();
        if (this.svInterval.withStrand(Strand.FWD).getEndPos() > MAX)
            MAX = this.svInterval.withStrand(Strand.FWD).getEndPos();
        this.span = MAX - MIN;
        if (span<0)
            throw new SvAnnRuntimeException("Malformed svg case todo figure out");
        this.offset = (int) (0.1*span);
        int left = this.svInterval.withStrand(Strand.FWD).getBeginPos() - offset;
        int right = this.svInterval.withStrand(Strand.FWD).getEndPos() + offset;
        this.leftBoundary = left <0 ? 0: left;
        this.rightBoundary = right;
        if (genomeInterval.getChr() < 23)
            this.chrom = String.format("chr%d",genomeInterval.getChr());
        else if (genomeInterval.getChr() == 23)
            this.chrom = "chrX";
        else if (genomeInterval.getChr() == 24)
            this.chrom = "chrY";
        else if (genomeInterval.getChr() == 25)
            this.chrom = "chrM";
        else
            this.chrom = String.valueOf(genomeInterval.getChr()); // should never happen
        this.SVG_HEIGHT = HEIGHT_FOR_SV_DISPLAY + (enhancers.size() + transcripts.size()) * HEIGHT_PER_DISPLAY_ITEM;
    }

    /** Write the header of the SVG.
     */
    private void writeHeader(Writer writer, boolean blackBorder) throws IOException {
        writer.write("<svg width=\"" + this.SVG_WIDTH +"\" height=\""+ this.SVG_HEIGHT +"\" ");
        if (blackBorder) {
            writer.write("style=\"border:1px solid black\" " );
        }
        writer.write("style=\"border:1px solid black\" " +
                "xmlns=\"http://www.w3.org/2000/svg\" " +
                "xmlns:svg=\"http://www.w3.org/2000/svg\">\n");
        writer.write("<!-- Created by SvAnna -->\n");
        writer.write("<style>\n" +
                "  text { font: 24px; }\n" +
                "  text.t20 { font: 20px; }\n" +
                "  text.t14 { font: 14px; }\n" +
                "  </style>\n");
        writer.write("<g>\n");
    }

    private void writeHeader(Writer writer) throws IOException {
        writeHeader(writer, false);
    }

    /** Write the footer of the SVG */
    private void writeFooter(Writer writer) throws IOException {
        writer.write("</g>\n</svg>\n");
    }

    /**
     * Transform a genomic cooordinate to an SVG X coordinate
     * @return
     */
    private double translateGenomicToSvg(int genomicCoordinate) {

        return 42.0;
    }


    public String getSvg() {
        return "";
    }
}
