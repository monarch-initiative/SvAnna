package org.jax.svann.viz.svg;

import com.google.common.collect.Lists;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Structural variant (SV) Scalar Vector Graphic (SVG) generator.
 * @author Peter N Robinson
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
    /** Boundaries of SVG we do not write to. */
    private final double OFFSET_FACTOR = 0.1;

    private final double SVG_OFFSET = SVG_WIDTH * OFFSET_FACTOR;
    /** Number of base pairs from left to right boundary */
    private final double genomicSpan;
    /** Left most position, including offset. */
    private final int genomicMinPos;
    private final int genomicMaxPos;
    /** This will be 10% of genomicSpan, extra area on the sides of the graphic to make things look nicer. */
    private final int genomicOffset;

    final String pattern = "###,###.###";
    final DecimalFormat decimalFormat = new DecimalFormat(pattern);

    private final String variantDescription;
    private final String chrom;

    private final double INTRON_MIDPOINT_ELEVATION = 10.0;

    private final double EXON_HEIGHT = 20;

    private final double SV_HEIGHT = 30;

    public final static String PURPLE = "#790079";
    public final static String GREEN = "#00A087";
    public final static String DARKGREEN = "#006600";
    public final static String RED ="#e64b35";
    public final static String BLACK = "#000000";
    public final static String NEARLYBLACK = "#040C04";

    public final static String BLUE ="#4dbbd5";

    public final static String BROWN="#7e6148";
    public final static String DARKBLUE = "#3c5488";
    public final static String VIOLET = "#8491b4";
    public final static String ORANGE = "#ff9900";


    public final static String BRIGHT_GREEN = "#00a087";

    /**
     * The constructor calculates the left and right boundaries for display
     * TODO document logic, cleanup
     * @param transcripts
     * @param enhancers
     * @param genomeInterval
     */
    public SvSvgGenerator(List<TranscriptModel> transcripts,
                          List<Enhancer> enhancers,
                          GenomeInterval genomeInterval,
                          String varString) {
        this.affectedTranscripts = transcripts;
        this.affectedEnhancers = enhancers;
        this.svInterval = genomeInterval;
        this.variantDescription = varString;

        int MINtranscript = Integer.MAX_VALUE;
        int MAXtranscript = Integer.MIN_VALUE;
        for (var tmod : affectedTranscripts) {
            int begin = tmod.getTXRegion().withStrand(Strand.FWD).getBeginPos();
            int end = tmod.getTXRegion().withStrand(Strand.FWD).getEndPos();
            if (begin < MINtranscript) MINtranscript = begin;
            if (end > MAXtranscript) MAXtranscript = end;
        }
        for (var e : affectedEnhancers) {
            int begin = e.getStart().getPos();
            int end = e.getEnd().getPos();
            if (begin > end) throw new SvAnnRuntimeException("blablsbas");
            if (begin < MINtranscript) MINtranscript = begin;
            if (end > MAXtranscript) MAXtranscript = end;
        }
        int MINsv = this.svInterval.withStrand(Strand.FWD).getBeginPos();
        int MAXsv = this.svInterval.withStrand(Strand.FWD).getEndPos();
        int MIN = Math.min(MINtranscript,  MINsv);
        int MAX = Math.max(MAXtranscript,  MAXsv);
        int DELTA = MAX - MIN;
        this.genomicOffset = (int) (OFFSET_FACTOR* DELTA);
        this.genomicMinPos = MIN - genomicOffset;
        this.genomicMaxPos = MAX + genomicOffset;
        this.genomicSpan = this.genomicMaxPos - this.genomicMinPos;
        if (genomicSpan <0)
            throw new SvAnnRuntimeException("Error, genomic span less than zero");
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
        writer.write(
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
        writeHeader(writer, true);
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
        double pos = genomicCoordinate - this.genomicMinPos;
        if (pos < 0) {
            throw new SvAnnRuntimeException("Bad left boundary (genomic coordinate-"); // should never happen
        }
        double prop = pos / genomicSpan;
        return prop * SVG_WIDTH;
    }



    /**
     * Write a line to indicate transcript (UTR) or a dotted line to indicate introns. The line forms
     * a triangle (inspired by the way Ensembl represents introns).
     * @param exons list of exons in sorted order (chromosome 5' to 3')
     * @param ypos vertical midline
     * @throws IOException if we cannot write
     */
    private void writeIntrons(List<GenomeInterval> exons, int ypos, Writer writer) throws  IOException {
        // if the gene does not have an intron, we are done
        if (exons.size() == 1)
            return;
        List<Integer> intronStarts = new ArrayList<>();
        List<Integer> intronEnds = new ArrayList<>();
        for (int i=1; i<exons.size(); i++) {
            GenomeInterval previous = exons.get(i-1).withStrand(Strand.FWD);
            GenomeInterval current = exons.get(i).withStrand(Strand.FWD);
            intronStarts.add(previous.getEndPos() + 1);
            intronEnds.add(current.getBeginPos() - 1);
        }
        for (int i=0; i<intronStarts.size(); i++) {
            double startpos = translateGenomicToSvg(intronStarts.get(i));
            double endpos   = translateGenomicToSvg(intronEnds.get(i));
            double midpoint = 0.5*(startpos + endpos);
            double Y = ypos;
            writer.write(String.format("<line x1=\"%f\" y1=\"%f\" x2=\"%f\" y2=\"%f\" stroke=\"black\"/>\n",
                    startpos, Y, midpoint, Y-INTRON_MIDPOINT_ELEVATION));
            writer.write(String.format("<line x1=\"%f\" y1=\"%f\" x2=\"%f\" y2=\"%f\" stroke=\"black\"/>\n",
                    midpoint, Y-INTRON_MIDPOINT_ELEVATION, endpos, Y));
        }
    }

    /**
     * Write a coding exon
     * @param start
     * @param end
     * @param ypos
     * @param writer
     * @throws IOException
     */
    private void writeCdsExon(double start, double end, int ypos, Writer writer) throws IOException {
        double width = end - start;
        double Y = ypos - 0.5*EXON_HEIGHT;
        String rect = String.format("<rect x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" rx=\"2\" " +
                        "style=\"stroke:%s; fill: %s\" />\n",
                start, Y, width, EXON_HEIGHT, DARKGREEN, GREEN);
        writer.write(rect);
    }

    /**
     * WRite a non-coding (i.e., UTR) exon of a non-coding gene
     * @param start
     * @param end
     * @param ypos
     * @param writer
     * @throws IOException
     */
    private void writeUtrExon(double start, double end, int ypos, Writer writer) throws IOException {
        double width = end - start;
        double Y = ypos - 0.5*EXON_HEIGHT;
        String rect = String.format("<rect x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" rx=\"2\" " +
                        "style=\"stroke:%s; fill: %s\" />\n",
                start, Y, width, EXON_HEIGHT, DARKGREEN, ORANGE);
        writer.write(rect);
    }

    /**
     * PROTOTYPE -- THIS MAYBE NOT BE THE BEST WAY TO REPRESENT OTHER TUPES OF SV
     * @param gi a genomic interval representing the SV
     * @param ypos  The y position where we will write the cartoon
     * @param msg A String describing the SV
     * @param writer a filehandle
     * @throws IOException if we cannt write
     */
    private void writeDeletion(GenomeInterval gi, int ypos, String msg, Writer writer) throws IOException {
        double start = translateGenomicToSvg(gi.getBeginPos());
        double end = translateGenomicToSvg(gi.getEndPos());
        double width = end - start;
        double Y = ypos + 0.5 * SV_HEIGHT;
        String rect = String.format("<rect x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" rx=\"2\" " +
                        "style=\"stroke:%s; fill: %s\" />\n",
                start, Y, width, SV_HEIGHT, DARKGREEN, RED);
        writer.write(rect);
        Y += 1.75*SV_HEIGHT;
        writer.write(String.format("<text x=\"%f\" y=\"%f\"  fill=\"%s\">%s</text>\n",start -10,Y, PURPLE, msg));
    }

    /**
     * This method writes one Jannovar transcript as a cartoon where the UTRs are shown in one color and the
     * the coding exons are shown in another color. TODO -- decide what to do with non-coding genes
     * @param tmod The Jannovar representation of a transcript
     * @param ypos The y position where we will write the cartoon
     * @param writer file handle
     * @throws IOException if we cannot write.
     */
    private void writeTranscript(TranscriptModel tmod, int ypos, Writer writer) throws IOException {
        GenomeInterval cds = tmod.getCDSRegion().withStrand(Strand.FWD);
        GenomeInterval tx = tmod.getTXRegion().withStrand(Strand.FWD);
        double cdsStart = translateGenomicToSvg(cds.getBeginPos());
        double cdsEnd = translateGenomicToSvg(cds.getEndPos());
        List<GenomeInterval> exons = tmod.getExonRegions();
        if (tmod.getStrand() == Strand.REV) {
            exons = Lists.reverse(exons);
        }
        // write a line for UTR, otherwise write a box
        for (GenomeInterval exon : exons) {
            double exonStart = translateGenomicToSvg(exon.withStrand(Strand.FWD).getBeginPos());
            double exonEnd = translateGenomicToSvg(exon.withStrand(Strand.FWD).getEndPos());
            if (exonStart>= cdsStart && exonEnd <= cdsEnd) {
                writeCdsExon(exonStart, exonEnd, ypos, writer);
            } else if (exonStart <= cdsEnd && exonEnd > cdsEnd) {
                // in this case, the 3' portion of the exon is UTR and the 5' is CDS
                writeCdsExon(exonStart, cdsEnd, ypos, writer);
                writeUtrExon(cdsEnd, exonEnd, ypos, writer);
            } else if (exonStart < cdsStart && exonEnd > cdsStart) {
                writeUtrExon(exonStart, cdsStart, ypos, writer);
                writeCdsExon(cdsStart, exonEnd, ypos, writer);
            } else {
                writeUtrExon(exonStart, exonEnd, ypos, writer);
            }
        }
        writeIntrons(exons, ypos, writer);
    }

    /**
     * Get a string containing an SVG representing the SV.
     * @return an SVG string
     */
    public String getSvg() {
        StringWriter swriter = new StringWriter();
        try {
            writeHeader(swriter);
            write(swriter);
            writeFooter(swriter);
            return swriter.toString();
        } catch (IOException e) {
            return getSvgErrorMessage(e.getMessage());
        }
    }

    /**
     * Wirte an SVG (without header) representing this SV. Not intended to be used to create a stand-alone
     * SVG (for this, user {@link #getSvg()}
     * @param writer a file handle
     * @throws IOException if we cannot write.
     */
    public void write(Writer writer) throws IOException {
        int starty = 50;
        int y = starty;
        writeDeletion(this.svInterval, starty, this.variantDescription, writer);
        y += 100;
        for (var tmod : this.affectedTranscripts) {
            writeTranscript(tmod, y, writer);
            y += HEIGHT_PER_DISPLAY_ITEM;
        }
    }


    /**
     * If there is some IO Exception, return an SVG with a text that indicates the error
     * @param msg The error
     * @return An SVG element that contains the error
     */
    protected String getSvgErrorMessage(String msg) {
        return String.format("<svg width=\"200\" height=\"100\" " +
                "xmlns=\"http://www.w3.org/2000/svg\" " +
                "xmlns:svg=\"http://www.w3.org/2000/svg\">\n" +
                "<!-- Created by SvAnna -->\n" +
                "<g><text x=\"10\" y=\"10\">%s</text>\n</g>\n" +
                "</svg>\n", msg);
    }


}
