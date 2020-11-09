package org.jax.svann.viz.svg;

import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
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
                min().orElse(0);
        this.minTranscriptPosB = transcriptModelsB.stream().
                map(TranscriptModel::getTXRegion).
                map(gi -> gi.withStrand(Strand.FWD)).
                mapToInt(GenomeInterval::getBeginPos).
                min().orElse(0);
        this.maxTranscriptPosA = transcriptModelsA.stream().
                map(TranscriptModel::getTXRegion).
                map(gi -> gi.withStrand(Strand.FWD)).
                mapToInt(GenomeInterval::getBeginPos).
                max().orElse(0);
        this.maxTranscriptPosB = transcriptModelsB.stream().
                map(TranscriptModel::getTXRegion).
                map(gi -> gi.withStrand(Strand.FWD)).
                mapToInt(GenomeInterval::getBeginPos).
                max().orElse(0);
        this.minEnhancerPosA = enhancersA.stream()
                .map(Enhancer::getStart)
                .mapToInt(GenomicPosition::getPosition)
                .min().orElse(0);
        this.minEnhancerPosB = enhancersB.stream()
                .map(Enhancer::getStart)
                .mapToInt(GenomicPosition::getPosition)
                .min().orElse(0);
        this.maxEnhancerPosA = enhancersA.stream()
                .map(Enhancer::getStart)
                .mapToInt(GenomicPosition::getPosition)
                .max().orElse(0);
        this.maxEnhancerPosB = enhancersB.stream()
                .map(Enhancer::getStart)
                .mapToInt(GenomicPosition::getPosition)
                .max().orElse(0);
        this.minPosA = Math.min(this.minEnhancerPosA, this.minTranscriptPosA);
        this.maxPosA = Math.max(this.maxEnhancerPosA, this.maxTranscriptPosA);
        this.minPosB = Math.min(this.minEnhancerPosB, this.minTranscriptPosB);
        this.maxPosB = Math.max(this.maxEnhancerPosB, this.maxTranscriptPosB);

    }






    @Override
    public void write(Writer writer) throws IOException {
        int starty = 50;
        int y = starty;
//        double start = translateGenomicToSvg(this.insertionCoordinates.getStartPosition());
//        double end = translateGenomicToSvg(this.insertionCoordinates.getEndPosition());
    }
}
