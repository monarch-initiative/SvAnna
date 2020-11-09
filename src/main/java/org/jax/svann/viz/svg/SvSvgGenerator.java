package org.jax.svann.viz.svg;

import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.*;
import org.jax.svann.reference.transcripts.SvAnnTxModel;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Structural variant (SV) Scalar Vector Graphic (SVG) generator.
 *
 * @author Peter N Robinson
 */
public abstract class SvSvgGenerator {

    public final static String PURPLE = "#790079";
    public final static String GREEN = "#00A087";
    public final static String DARKGREEN = "#006600";
    public final static String RED = "#e64b35";
    public final static String BLACK = "#000000";
    public final static String NEARLYBLACK = "#040C04";
    public final static String BLUE = "#4dbbd5";
    public final static String BROWN = "#7e6148";
    public final static String DARKBLUE = "#3c5488";
    public final static String VIOLET = "#8491b4";
    public final static String ORANGE = "#ff9900";
    public final static String BRIGHT_GREEN = "#00a087";
    protected final static int HEIGHT_PER_DISPLAY_ITEM = 100;
    private final static int HEIGHT_FOR_SV_DISPLAY = 200;
    protected final List<SvAnnTxModel> affectedTranscripts;
    protected final List<Enhancer> affectedEnhancers;
    protected final List<CoordinatePair> coordinatePairs;
    /**
     * Leftmost position (most 5' on chromosome), including offset.
     */
    protected final int genomicMinPos;
    /**
     * Rightmost position (most 3' on chromosome), including offset.
     */
    protected final int genomicMaxPos;
    /**
     * Height of the symbol that represents the structural variant.
     */
    protected final double SV_HEIGHT = 30;
    final String pattern = "###,###.###";
    final DecimalFormat decimalFormat = new DecimalFormat(pattern);
    /**
     * Canvas width of the SVG
     */
    private final int SVG_WIDTH = 1400;
    /**
     * Boundaries of SVG we do not write to.
     */
    private final double OFFSET_FACTOR = 0.1;
    private final double SVG_OFFSET = SVG_WIDTH * OFFSET_FACTOR;
    /**
     * Number of base pairs from left to right boundary
     */
    private final double genomicSpan;
    /**
     * Minimum position of the scale
     */
    private final double scaleMinPos;
    private final double scaleMaxPos;
    private final int scaleBasePairs;
    private final double INTRON_MIDPOINT_ELEVATION = 10.0;
    /**
     * Height of the symbols that represent the transcripts
     */
    private final double EXON_HEIGHT = 20;
    /**
     * Y skip to put text underneath transcripts. Works with {@link #writeTranscriptName}
     */
    private final double Y_SKIP_BENEATH_TRANSCRIPTS = 50;
    private final SvType svtype;
    /**
     * Canvas height of the SVG.
     */
    protected int SVG_HEIGHT;
    /**
     * This will be 10% of genomicSpan, extra area on the sides of the graphic to make things look nicer.
     */
    private int genomicOffset;
    private String variantDescription;
    private String chrom;


    /**
     * The constructor calculates the left and right boundaries for display
     * TODO document logic, cleanup
     *
     * @param transcripts
     * @param enhancers   // * @param genomeInterval
     */
    public SvSvgGenerator(SvType svtype,
                          List<SvAnnTxModel> transcripts,
                          List<Enhancer> enhancers,
                          List<CoordinatePair> coordinatePairs) {
        this.svtype = svtype;
        this.affectedTranscripts = transcripts;
        this.affectedEnhancers = enhancers;
        this.coordinatePairs = coordinatePairs;
        this.SVG_HEIGHT = HEIGHT_FOR_SV_DISPLAY + (enhancers.size() + transcripts.size()) * HEIGHT_PER_DISPLAY_ITEM;
        switch (svtype) {
            case DELETION:
            case INSERTION:
            default:
                // get min/max for SVs with one region
                // todo  -- do we need to check how many coordinate pairs there are?
                CoordinatePair cpair = coordinatePairs.get(0);
                int minPos = getGenomicMinPos(transcripts, enhancers, cpair);
                int maxPos = getGenomicMaxPos(transcripts, enhancers, cpair);

                // add a little real estate to each side for esthetic purposes
                int delta = maxPos - minPos;
                int offset = (int) (OFFSET_FACTOR * delta);
                this.genomicMinPos = Math.max(minPos - offset, 0);
                this.genomicMaxPos = maxPos + offset; // don't care if we fall off the end, this is not important for visualization
                this.genomicSpan = this.genomicMaxPos - this.genomicMinPos;
                this.scaleBasePairs = 1 + maxPos - minPos;
                this.scaleMinPos = translateGenomicToSvg(minPos);
                this.scaleMaxPos = translateGenomicToSvg(maxPos);
        }
    }

    /**
     * For plotting SVs, we need to know the minimum and maximum genomic position. We do this with this method
     * (rather than taking say the enahncers from {@link #affectedEnhancers}, because for some types of
     * SVs like translocations, we will only use part of the list to calculate maximum and minimum positions.
     *
     * @param transcripts Transcripts on the same chromosome as cpair that overlap with the entire or in some cases a component of the SV
     * @param enhancers   Enhancers on the same chromosome as cpair that overlap with the entire or in some cases a component of the SV
     * @param cpair       coordinates of a structural variant or (in some cases such as Translocations) a component of a SV
     * @return minimum coordinate (i.e., most 5' coordinate)
     */
    int getGenomicMinPos(List<SvAnnTxModel> transcripts,
                         List<Enhancer> enhancers,
                         CoordinatePair cpair) {
        int minPos = cpair.getStart().withStrand(org.jax.svann.reference.Strand.FWD).getPosition();
        return getGenomicMinPos(transcripts, enhancers, minPos);
    }


    int getGenomicMinPos(List<SvAnnTxModel> transcripts,
                         List<Enhancer> enhancers,
                         int pos) {
        int transcriptMin = transcripts.stream().
                map(t -> t.withStrand(org.jax.svann.reference.Strand.FWD)).
                mapToInt(GenomicRegion::getStartPosition).
                min().
                orElse(Integer.MAX_VALUE);
        int enhancerMin = enhancers.stream().
                map(Enhancer::getStart).
                mapToInt(GenomicPosition::getPosition).
                min().
                orElse(Integer.MAX_VALUE);
        return Math.min(transcriptMin, Math.min(enhancerMin, pos));
    }


    /**
     * For plotting SVs, we need to know the minimum and maximum genomic position. We do this with this method
     * (rather than taking say the enahncers from {@link #affectedEnhancers}, because for some types of
     * SVs like translocations, we will only use part of the list to calculate maximum and minimum positions.
     *
     * @param transcripts Transcripts on the same chromosome as cpair that overlap with the entire or in some cases a component of the SV
     * @param enhancers   Enhancers on the same chromosome as cpair that overlap with the entire or in some cases a component of the SV
     * @param cpair       coordinates of a structural variant or (in some cases such as Translocations) a component of a SV
     * @return minimum coordinate (i.e., most 5' coordinate)
     */
    int getGenomicMaxPos(List<SvAnnTxModel> transcripts,
                         List<Enhancer> enhancers,
                         CoordinatePair cpair) {
        int maxPos = cpair.getEnd().withStrand(org.jax.svann.reference.Strand.FWD).getPosition();
        return getGenomicMaxPos(transcripts, enhancers, maxPos);
    }

    int getGenomicMaxPos(List<SvAnnTxModel> transcripts,
                         List<Enhancer> enhancers,
                         int pos) {
        int transcriptMax = transcripts.stream().map(t -> t.withStrand(org.jax.svann.reference.Strand.FWD))
                .mapToInt(GenomicRegion::getEndPosition)
                .min()
                .orElse(Integer.MIN_VALUE);
        int enhancerMax = enhancers.stream().
                map(Enhancer::getEnd).
                mapToInt(GenomicPosition::getPosition).
                min().
                orElse(Integer.MIN_VALUE);
        return Math.max(transcriptMax, Math.max(enhancerMax, pos));
    }


    /**
     * Write the header of the SVG.
     */
    private void writeHeader(Writer writer, boolean blackBorder) throws IOException {
        writer.write("<svg width=\"" + this.SVG_WIDTH + "\" height=\"" + this.SVG_HEIGHT + "\" ");
        if (blackBorder) {
            writer.write("style=\"border:1px solid black\" ");
        }
        writer.write(
                "xmlns=\"http://www.w3.org/2000/svg\" " +
                        "xmlns:svg=\"http://www.w3.org/2000/svg\">\n");
        writer.write("<!-- Created by SvAnna -->\n");
        writer.write("<style>\n" +
                "  text { font: 24px; }\n" +
                "  text.t20 { font: 20px; }\n" +
                "  text.t14 { font: 14px; }\n");
        writer.write("  .mytriangle{\n" +
                "    margin: 0 auto;\n" +
                "    width: 100px;\n" +
                "    height: 100px;\n" +
                "  }\n" +
                "\n" +
                "  .mytriangle polygon {\n" +
                "    fill:#b31900;\n" +
                "    stroke:#65b81d;\n" +
                "    stroke-width:2;\n" +
                "  }\n");
        writer.write("  </style>\n");
        writer.write("<g>\n");
    }

    private void writeHeader(Writer writer) throws IOException {
        writeHeader(writer, true);
    }

    /**
     * Write the footer of the SVG
     */
    private void writeFooter(Writer writer) throws IOException {
        writer.write("</g>\n</svg>\n");
    }

    /**
     * Transform a genomic cooordinate to an SVG X coordinate
     *
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
     *
     * @param exons list of exons in sorted order (chromosome 5' to 3')
     * @param ypos  vertical midline
     * @throws IOException if we cannot write
     */
    private void writeIntrons(List<GenomicRegion> exons, int ypos, Writer writer) throws IOException {
        // if the gene does not have an intron, we are done
        if (exons.size() == 1)
            return;
        List<Integer> intronStarts = new ArrayList<>();
        List<Integer> intronEnds = new ArrayList<>();
        for (int i = 1; i < exons.size(); i++) {
            GenomicRegion previous = exons.get(i - 1).withStrand(Strand.FWD);
            GenomicRegion current = exons.get(i).withStrand(Strand.FWD);
            intronStarts.add(previous.getEndPosition() + 1);
            intronEnds.add(current.getStartPosition() - 1);
        }
        for (int i = 0; i < intronStarts.size(); i++) {
            double startpos = translateGenomicToSvg(intronStarts.get(i));
            double endpos = translateGenomicToSvg(intronEnds.get(i));
            double midpoint = 0.5 * (startpos + endpos);
            double Y = ypos;
            writer.write(String.format("<line x1=\"%f\" y1=\"%f\" x2=\"%f\" y2=\"%f\" stroke=\"black\"/>\n",
                    startpos, Y, midpoint, Y - INTRON_MIDPOINT_ELEVATION));
            writer.write(String.format("<line x1=\"%f\" y1=\"%f\" x2=\"%f\" y2=\"%f\" stroke=\"black\"/>\n",
                    midpoint, Y - INTRON_MIDPOINT_ELEVATION, endpos, Y));
        }
    }

    /**
     * Write a coding exon
     *
     * @param start
     * @param end
     * @param ypos
     * @param writer
     * @throws IOException
     */
    private void writeCdsExon(double start, double end, int ypos, Writer writer) throws IOException {
        double width = end - start;
        double Y = ypos - 0.5 * EXON_HEIGHT;
        String rect = String.format("<rect x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" rx=\"2\" " +
                        "style=\"stroke:%s; fill: %s\" />\n",
                start, Y, width, EXON_HEIGHT, DARKGREEN, GREEN);
        writer.write(rect);
    }

    /**
     * WRite a non-coding (i.e., UTR) exon of a non-coding gene
     *
     * @param start
     * @param end
     * @param ypos
     * @param writer
     * @throws IOException
     */
    private void writeUtrExon(double start, double end, int ypos, Writer writer) throws IOException {
        double width = end - start;
        double Y = ypos - 0.5 * EXON_HEIGHT;
        String rect = String.format("<rect x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" rx=\"2\" " +
                        "style=\"stroke:%s; fill: %s\" />\n",
                start, Y, width, EXON_HEIGHT, DARKGREEN, ORANGE);
        writer.write(rect);
    }


    /**
     * This method writes one Jannovar transcript as a cartoon where the UTRs are shown in one color and the
     * the coding exons are shown in another color. TODO -- decide what to do with non-coding genes
     *
     * @param tmod   The Jannovar representation of a transcript
     * @param ypos   The y position where we will write the cartoon
     * @param writer file handle
     * @throws IOException if we cannot write.
     */
    protected void writeTranscript(SvAnnTxModel tmod, int ypos, Writer writer) throws IOException {
        GenomicRegion cds = StandardGenomicRegion.of(tmod.getCdsStart(), tmod.getCdsEnd()).withStrand(Strand.FWD);

        double cdsStart = translateGenomicToSvg(cds.getStartPosition());
        double cdsEnd = translateGenomicToSvg(cds.getEndPosition());
        List<GenomicRegion> exons = tmod.getExonRegions();
        if (tmod.getStrand() == Strand.REV) {
            Comparator<GenomicRegion> tComparator = Comparator.naturalOrder();
            exons = exons.stream()
                    .sorted(tComparator.reversed())
                    .collect(Collectors.toList());
        }
        double minX = Double.MAX_VALUE;
        // write a line for UTR, otherwise write a box
        for (GenomicRegion exon : exons) {
            double exonStart = translateGenomicToSvg(exon.withStrand(Strand.FWD).getStartPosition());
            double exonEnd = translateGenomicToSvg(exon.withStrand(Strand.FWD).getEndPosition());
            if (exonStart < minX) minX = exonStart;
            if (exonStart >= cdsStart && exonEnd <= cdsEnd) {
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
        writeTranscriptName(tmod, minX, ypos, writer);
    }

    private void writeTranscriptName(SvAnnTxModel tmod, double xpos, int ypos, Writer writer) throws IOException {
        String symbol = tmod.getGeneSymbol();
        String accession = tmod.getAccession();
        String chrom = tmod.getContig().getPrimaryName();
        SvAnnTxModel txOnFwdStrand = tmod.withStrand(Strand.FWD);
        int start = txOnFwdStrand.getStartPosition();
        int end = txOnFwdStrand.getEndPosition();
        String strand = tmod.getStrand() == Strand.FWD ? "+" : "-";
        String positionString = String.format("%s:%d-%d (%s strand)", chrom, start, end, strand);
        String geneName = String.format("%s (%s)", symbol, accession);
        double y = Y_SKIP_BENEATH_TRANSCRIPTS + ypos;
        String txt = String.format("<text x=\"%f\" y=\"%f\" fill=\"%s\">%s</text>\n",
                xpos, y, PURPLE, String.format("%s  %s", geneName, positionString));
        writer.write(txt);
    }


    protected void writeScale(Writer writer, int ypos) throws IOException {
        int verticalOffset = 10;
        String line = String.format("<line x1=\"%f\" y1=\"%d\"  x2=\"%f\"  y2=\"%d\" style=\"stroke: #000000; fill:none;" +
                " stroke-width: 1px;" +
                " stroke-dasharray: 5 2\" />\n", this.scaleMinPos, ypos, this.scaleMaxPos, ypos);
        String leftVertical = String.format("<line x1=\"%f\" y1=\"%d\"  x2=\"%f\"  y2=\"%d\" style=\"stroke: #000000; fill:none;" +
                " stroke-width: 1px;\" />\n", this.scaleMinPos, ypos + verticalOffset, this.scaleMinPos, ypos - verticalOffset);
        String rightVertical = String.format("<line x1=\"%f\" y1=\"%d\"  x2=\"%f\"  y2=\"%d\" style=\"stroke: #000000; fill:none;" +
                " stroke-width: 1px;\" />\n", this.scaleMaxPos, ypos + verticalOffset, this.scaleMaxPos, ypos - verticalOffset);
        String sequenceLength = getSequenceLengthString(scaleBasePairs);
        writer.write(line);
        writer.write(leftVertical);
        writer.write(rightVertical);
        int y = ypos - 15;
        double xmiddle = 0.45 * (this.scaleMinPos + this.scaleMaxPos);
        String txt = String.format("<text x=\"%f\" y=\"%d\" fill=\"%s\">%s</text>\n",
                xmiddle, y, PURPLE, sequenceLength);
        writer.write(txt);
    }

    /**
     * Get a string that represents a sequence length using bp, kb, or Mb as appropriate
     *
     * @param seqlen number of base bairs
     * @return String such as 432 bp, 4.56 kb or 1.23 Mb
     */
    protected String getSequenceLengthString(int seqlen) {
        if (seqlen < 1_000) {
            return String.format("%d bp", seqlen);
        } else if (seqlen < 1_000_000) {
            double kb = (double) seqlen / 1000.0;
            return String.format("%.2f kp", kb);
        } else {
            // if we get here, the sequence is at least one million bp
            double mb = (double) seqlen / 1000000.0;
            return String.format("%.2f Mp", mb);
        }
    }

    /**
     * Get a string containing an SVG representing the SV.
     *
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
     *
     * @param writer a file handle
     * @throws IOException if we cannot write.
     */
    public abstract void write(Writer writer) throws IOException;

    /**
     * If there is some IO Exception, return an SVG with a text that indicates the error
     *
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
