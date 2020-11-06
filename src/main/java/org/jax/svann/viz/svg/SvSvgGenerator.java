package org.jax.svann.viz.svg;

import com.google.common.collect.Lists;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.SvType;

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
public abstract class SvSvgGenerator {

    /** Canvas width of the SVG */
    private final int SVG_WIDTH = 1400;

    private final static int HEIGHT_FOR_SV_DISPLAY = 200;
    protected final static int HEIGHT_PER_DISPLAY_ITEM = 100;
    /** Canvas height of the SVG.*/
    protected int SVG_HEIGHT;

    protected final List<TranscriptModel> affectedTranscripts;
    protected final List<Enhancer> affectedEnhancers;
    protected final List<CoordinatePair> coordinatePairs;

    /** Boundaries of SVG we do not write to. */
    private final double OFFSET_FACTOR = 0.1;

    private final double SVG_OFFSET = SVG_WIDTH * OFFSET_FACTOR;
    /** Number of base pairs from left to right boundary */
    private final double genomicSpan;
    /** Leftmost position (most 5' on chromosome), including offset. */
    protected final int genomicMinPos;
    /** Rightmost position (most 3' on chromosome), including offset. */
    protected final int genomicMaxPos;
    /** This will be 10% of genomicSpan, extra area on the sides of the graphic to make things look nicer. */
    private int genomicOffset;

    final String pattern = "###,###.###";
    final DecimalFormat decimalFormat = new DecimalFormat(pattern);

    private String variantDescription;
    private String chrom;

    private final double INTRON_MIDPOINT_ELEVATION = 10.0;

    private final double EXON_HEIGHT = 20;
    /** Height of the symbol that represents the structural variant. */
    protected final double SV_HEIGHT = 30;

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


    private final SvType svtype;


    /**
     * The constructor calculates the left and right boundaries for display
     * TODO document logic, cleanup
     *
     * @param transcripts
     * @param enhancers   // * @param genomeInterval
     */
    public SvSvgGenerator(SvType svtype,
                          List<TranscriptModel> transcripts,
                          List<Enhancer> enhancers,
                          List<CoordinatePair> coordinatePairs) {
        this.svtype = svtype;
        this.affectedTranscripts = transcripts;
        this.affectedEnhancers = enhancers;
        this.coordinatePairs = coordinatePairs;
        this.SVG_HEIGHT = HEIGHT_FOR_SV_DISPLAY + (enhancers.size() + transcripts.size()) * HEIGHT_PER_DISPLAY_ITEM;
        switch (svtype) {
            case DELETION:
            default:
                // get min/max for SVs with one region
                // todo  -- do we need to check how many coordinate pairs there are?
                CoordinatePair cpair = coordinatePairs.get(0);
                int minPos = getGenomicMinPos(transcripts, enhancers, cpair);
                int maxPos = getGenomicMaxPos(transcripts, enhancers, cpair);
                // add a little real estate to each side for esthetic purposes
                int delta = maxPos - minPos;
                int offset = (int) (OFFSET_FACTOR * delta);
                this.genomicMinPos = minPos - offset < 0 ? 0 : minPos - offset;
                this.genomicMaxPos = maxPos + offset; // don't care if we fall off the end, this is not important for visualization
                this.genomicSpan = this.genomicMaxPos - this.genomicMinPos;
        }
    }

    /**
     * For plotting SVs, we need to know the minimum and maximum genomic position. We do this with this method
     * (rather than taking say the enahncers from {@link #affectedEnhancers}, because for some types of
     * SVs like translocations, we will only use part of the list to calculate maximum and minimum positions.
     * @param transcripts Transcripts on the same chromosome as cpair that overlap with the entire or in some cases a component of the SV
     * @param enhancers Enhancers on the same chromosome as cpair that overlap with the entire or in some cases a component of the SV
     * @param cpair coordinates of a structural variant or (in some cases such as Translocations) a component of a SV
     * @return minimum coordinate (i.e., most 5' coordinate)
     */
    int getGenomicMinPos(List<TranscriptModel> transcripts,
                         List<Enhancer> enhancers,
                         CoordinatePair cpair) {
        int transcriptMin = transcripts.stream().
                map(TranscriptModel::getTXRegion).
                map(t -> t.withStrand(Strand.FWD)).
                mapToInt(GenomeInterval::getBeginPos).
                min().
                orElse(Integer.MAX_VALUE);
        int enhancerMin = enhancers.stream().
                map(Enhancer::getStart).
                mapToInt(GenomicPosition::getPosition).
                min().
                orElse(Integer.MAX_VALUE);
        int minPair = cpair.getStart().withStrand(org.jax.svann.reference.Strand.FWD).getPosition();
        return Math.min(transcriptMin, Math.min(enhancerMin, minPair));
    }

    /**
     * For plotting SVs, we need to know the minimum and maximum genomic position. We do this with this method
     * (rather than taking say the enahncers from {@link #affectedEnhancers}, because for some types of
     * SVs like translocations, we will only use part of the list to calculate maximum and minimum positions.
     * @param transcripts Transcripts on the same chromosome as cpair that overlap with the entire or in some cases a component of the SV
     * @param enhancers Enhancers on the same chromosome as cpair that overlap with the entire or in some cases a component of the SV
     * @param cpair coordinates of a structural variant or (in some cases such as Translocations) a component of a SV
     * @return minimum coordinate (i.e., most 5' coordinate)
     */
    int getGenomicMaxPos(List<TranscriptModel> transcripts,
                         List<Enhancer> enhancers,
                         CoordinatePair cpair) {
        int transcriptMax = transcripts.stream().
                map(TranscriptModel::getTXRegion).
                map(t -> t.withStrand(Strand.FWD)).
                mapToInt(GenomeInterval::getEndPos).
                min().
                orElse(Integer.MIN_VALUE);
        int enhancerMax = enhancers.stream().
                map(Enhancer::getEnd).
                mapToInt(GenomicPosition::getPosition).
                min().
                orElse(Integer.MIN_VALUE);
        int maxPair = cpair.getEnd().withStrand(org.jax.svann.reference.Strand.FWD).getPosition();
        return Math.max(transcriptMax, Math.max(enhancerMax, maxPair));
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
    protected double translateGenomicToSvg(int genomicCoordinate) {
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
    protected void writeTranscript(TranscriptModel tmod, int ypos, Writer writer) throws IOException {
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
    public abstract void write(Writer writer) throws IOException;

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
