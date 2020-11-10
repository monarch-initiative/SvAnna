package org.jax.svann.viz.svg;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.transcripts.SvAnnTxModel;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class creates an SVG picture that illustrates the effects of a translocation. It shows each breakpoint separately
 * i.e., it shows two rows, one for each affected Contig. It does not attempt to show the resulting derived chromosomes.
 */
public class TranslocationSvgGenerator extends SvSvgGenerator {


    private final static int YSTART = 50;
    private final static int VERTICAL_INCREMENT_PER_DISPLAY_ITEM = HEIGHT_PER_DISPLAY_ITEM;
    private final int separationLineY;
    private final int ystartB;
    /**
     * One of the chromosomes affected by the translocation.
     */
    private final Contig contigA;
    /**
     * The other chromosome affected by the translocation.
     */
    private final Contig contigB;
    private final int positionContigA;

    private final int positionContigB;

    /**
     * transcripts on contig A
     */
    private final List<SvAnnTxModel> transcriptModelsA;
    /**
     * transcripts on contig B
     */
    private final List<SvAnnTxModel> transcriptModelsB;

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
     * The constructor calculates the left and right boundaries for display
     * TODO document logic, cleanup
     *
     * @param transcripts
     * @param enhancers       // * @param genomeInterval
     * @param coordinatePairs
     */
    public TranslocationSvgGenerator(SequenceRearrangement rearrangement,
                                     List<SvAnnTxModel> transcripts,
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
        this.transcriptModelsA = transcripts.stream().filter(t -> t.getContigId() == contigA.getId()).collect(Collectors.toList());
        this.transcriptModelsB = transcripts.stream().filter(t -> t.getContigId() == contigB.getId()).collect(Collectors.toList());
        this.enhancersA = enhancers.stream().filter(e -> e.getContig() == contigA).collect(Collectors.toList());
        this.enhancersB = enhancers.stream().filter(e -> e.getContig() == contigB).collect(Collectors.toList());
        int displayElementsA = transcriptModelsA.size() + enhancersA.size();
        int displayElementsB = transcriptModelsB.size() + enhancersB.size();
        this.separationLineY = displayElementsA*VERTICAL_INCREMENT_PER_DISPLAY_ITEM + YSTART + 100;
        this.ystartB = this.separationLineY + 50;

        int minA = getMin(transcriptModelsA, enhancersA, this.positionContigA);
        int maxA = getMax(transcriptModelsA, enhancersA, this.positionContigA);
        int minB = getMin(transcriptModelsB, enhancersB, this.positionContigB);
        int maxB = getMax(transcriptModelsB, enhancersB, this.positionContigB);
        this.componentA = new TranslocationComponentSvgGenerator(minA, maxA, transcriptModelsA, enhancersA, coordinatePairs, contigA, positionContigA, YSTART);
        this.componentB = new TranslocationComponentSvgGenerator(minB, maxB, transcriptModelsB, enhancersB, coordinatePairs, contigB, positionContigB, ystartB);
    }

    private int getMin(List<SvAnnTxModel> transcripts, List<Enhancer> enhancers, int contigPos) {
        contigPos = Math.max(0, contigPos - 1000); // add 1000 bp padding
        int minTranscriptPos = transcripts.stream().
                map(tx -> tx.withStrand(Strand.FWD)).
                mapToInt(SvAnnTxModel::getStartPosition).
                min().orElse(UNINITIALIZED);
        int minEnhancerPos = enhancers.stream()
                .map(Enhancer::getStart)
                .mapToInt(GenomicPosition::getPosition)
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

    private int getMax(List<SvAnnTxModel> transcripts, List<Enhancer> enhancers, int contigPos) {
        contigPos =  contigPos + 1000; // add 1000 bp padding
        int maxTranscriptPos = transcripts.stream().
                map(tx -> tx.withStrand(Strand.FWD)).
                mapToInt(SvAnnTxModel::getEndPosition).
                max().orElse(UNINITIALIZED);
        int maxEnhancerPos = enhancers.stream()
                .map(Enhancer::getEnd)
                .mapToInt(GenomicPosition::getPosition)
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
        int x2 = SVG_WIDTH - 20;
        String line = String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" style=\"stroke:rgb(192,192,192);stroke-width:1 stroke-dasharray: 5 2\" />",
                x1, ypos, x2, ypos);
        writer.write(line);
    }


    @Override
    public void write(Writer writer) throws IOException {
        this.componentA.write(writer);
        writeSeparationLine(this.separationLineY, writer);
        this.componentB.write(writer);
    }
}
