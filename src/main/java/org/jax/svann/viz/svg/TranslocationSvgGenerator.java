package org.jax.svann.viz.svg;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class creates an SVG picture that illustrates the effects of a translocation. It shows each breakpoint separately
 * i.e., it shows two rows, one for each affected Contig. It does not attempt to show the resulting derived chromosomes.
 */
public class TranslocationSvgGenerator extends SvSvgGenerator {
    /** One of the chromosomes affected by the translocation. */
    private final Contig contigA;
    /** The other chromosome affected by the translocation. */
    private final Contig contigB;
    private final int positionContigA;

    private final int positionContigB;

    /** transcripts on contig A */
    private final List<TranscriptModel> transcriptModelsA;
    /** transcripts on contig B  */
    private final List<TranscriptModel> transcriptModelsB;

    /** enhancers on contig A */
    private final List<Enhancer> enhancersA;
    /** enhancers on contig B */
    private final List<Enhancer> enhancersB;
    /** Flag to denote a value is not relevant. This may happen for instance if we have no enhancers. */
    private final static int UNINITIALIZED = -42;

    private final int minTranscriptPosA;
    private final int minTranscriptPosB;
    private final int maxTranscriptPosA;
    private final int maxTranscriptPosB;

    private final int minEnhancerPosA;
    private final int minEnhancerPosB;
    private final int maxEnhancerPosA;
    private final int maxEnhancerPosB;

    private final int minPosA;
    private final int minPosB;
    private final int maxPosA;
    private final int maxPosB;

    /**
     * The constructor calculates the left and right boundaries for display
     * TODO document logic, cleanup
     *
     * @param transcripts
     * @param enhancers       // * @param genomeInterval
     * @param coordinatePairs
     */
    public TranslocationSvgGenerator(SequenceRearrangement rearrangement,
                                     List<TranscriptModel> transcripts,
                                     List<Enhancer> enhancers,
                                     List<CoordinatePair> coordinatePairs) {
        super(SvType.TRANSLOCATION, transcripts, enhancers, coordinatePairs);
        // we want to collect the data for the two contigs
        // we assume that the left and right break ends have two different contigs but not that they may be
        // TODO can they be the same if we have a translocation in the same chromosome?
        this.contigA = rearrangement.getLeftmostBreakend().getContig();
        this.positionContigA = rearrangement.getLeftmostPosition();
        this.contigB = rearrangement.getRightmostBreakend().getContig();
        this.positionContigB = rearrangement.getRightmostPosition();
        this.transcriptModelsA = transcripts.stream().filter(t -> t.getChr() == contigA.getId()).collect(Collectors.toList());
        this.transcriptModelsB = transcripts.stream().filter(t -> t.getChr() == contigB.getId()).collect(Collectors.toList());
        this.enhancersA = enhancers.stream().filter(e -> e.getContig() == contigA).collect(Collectors.toList());
        this.enhancersB = enhancers.stream().filter(e -> e.getContig() == contigB).collect(Collectors.toList());
        this.minTranscriptPosA = transcriptModelsA.stream().
                map(TranscriptModel::getTXRegion).
                map(gi -> gi.withStrand(Strand.FWD)).
                mapToInt(GenomeInterval::getBeginPos).
                min().orElse(UNINITIALIZED);
        this.minTranscriptPosB = transcriptModelsB.stream().
                map(TranscriptModel::getTXRegion).
                map(gi -> gi.withStrand(Strand.FWD)).
                mapToInt(GenomeInterval::getBeginPos).
                min().orElse(UNINITIALIZED);
        this.maxTranscriptPosA = transcriptModelsA.stream().
                map(TranscriptModel::getTXRegion).
                map(gi -> gi.withStrand(Strand.FWD)).
                mapToInt(GenomeInterval::getEndPos).
                max().orElse(UNINITIALIZED);
        this.maxTranscriptPosB = transcriptModelsB.stream().
                map(TranscriptModel::getTXRegion).
                map(gi -> gi.withStrand(Strand.FWD)).
                mapToInt(GenomeInterval::getEndPos).
                max().orElse(UNINITIALIZED);
        this.minEnhancerPosA = enhancersA.stream()
                .map(Enhancer::getStart)
                .mapToInt(GenomicPosition::getPosition)
                .min().orElse(UNINITIALIZED);
        this.minEnhancerPosB = enhancersB.stream()
                .map(Enhancer::getStart)
                .mapToInt(GenomicPosition::getPosition)
                .min().orElse(UNINITIALIZED);
        this.maxEnhancerPosA = enhancersA.stream()
                .map(Enhancer::getEnd)
                .mapToInt(GenomicPosition::getPosition)
                .max().orElse(UNINITIALIZED);
        this.maxEnhancerPosB = enhancersB.stream()
                .map(Enhancer::getEnd)
                .mapToInt(GenomicPosition::getPosition)
                .max().orElse(UNINITIALIZED);

        this.minPosA = getMinimumInitializedValue(this.minTranscriptPosA, this.minEnhancerPosA);
        this.maxPosA = getMaximumInitializedValue(this.maxEnhancerPosA, this.maxTranscriptPosA);
        this.minPosB = getMinimumInitializedValue(this.minEnhancerPosB, this.minTranscriptPosB);
        this.maxPosB = getMaximumInitializedValue(this.maxEnhancerPosB, this.maxTranscriptPosB);

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


    private void writeZigZagLine(int y1, int y2, int x, Writer writer) throws IOException {
        int increment = 3;
        writer.write(String.format("<path d=\"M %d,%d \n", x, y1));
        int y = y1;
        while (y<y2) {
            writer.write(String.format(" l %d,%d\n",increment,increment));
            writer.write(String.format(" l %d,%d\n",-increment,increment));
            writer.write(String.format(" l %d,%d\n",-increment,increment));
            writer.write(String.format(" l %d,%d\n",increment,increment));
            y += 2*increment;
        }
        writer.write(" \" style=\"fill: red\" />\n");
    }

    /**
     * Transform a genomic cooordinate to an SVG X coordinate
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



    private int writeTranslocation(int ypos, String description, List<TranscriptModel> transcripts,
                                    List<Enhancer> enhancers, Writer writer) throws IOException {
        int ystart = ypos - 20;
        int offset = 0;
        for (var enh : enhancers) {
           // writeEnhancer(enh, ypos, writer);
            ypos += HEIGHT_PER_DISPLAY_ITEM;
            offset += HEIGHT_PER_DISPLAY_ITEM;
        }
        for (var tmod : transcripts) {
            writeTranscript(tmod, ypos, writer);
            ypos += HEIGHT_PER_DISPLAY_ITEM;
            offset += HEIGHT_PER_DISPLAY_ITEM;
        }
        int xpos = (int)translatePositionToSvg(this.positionContigA, this.minPosA, this.maxPosA);
        writeZigZagLine(ystart, ypos, xpos, writer);
        writeScale(writer, ypos);
        offset += 50; // for scale
        return offset;
    }

    private int writeSeparationLine(int ypos, Writer writer) throws IOException {
        int x1 = 20;
        int x2 = SVG_WIDTH - 20;
        String line = String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" style=\"stroke:rgb(255,0,0);stroke-width:2\" />",
                x1, ypos, x2, ypos);
        writer.write(line);
        return 20;
    }


    @Override
    public void write(Writer writer) throws IOException {
        int starty = 50;
        int y = starty;
        // first write contigA
//        double startA = translatePositionToSvg(this.minPosA, this.minPosA, this.maxPosA);
//        double endA = translatePositionToSvg(this.maxPosA, this.minPosA, this.maxPosA);
        String descriptionA = String.format("1. translocation breakpoint at %s:%d", contigA.getPrimaryName(), positionContigA);
        int offset = writeTranslocation(y, descriptionA, transcriptModelsA, enhancersA, writer);
        y += offset;
        offset = writeSeparationLine(y, writer);
        y += offset;
        // now write contig B
        String descriptionB = String.format("2. translocation breakpoint at %s:%d", contigB.getPrimaryName(), positionContigB);
        writeTranslocation(y, descriptionB, transcriptModelsB, enhancersB, writer);
    }
}
