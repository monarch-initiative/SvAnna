package org.jax.svann.viz.svg;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TranslocationSvgGenerator extends SvSvgGenerator {
    /** One of the chromosomes affected by the translocation. */
    private final Contig contigA;
    /** The other chromosome affected by the translocation. */
    private final Contig contigB;
    private final int positionContigA;

    private final int positionContigB;

    /** transcripts on the 5' side on contig A or the 3' side of contig B */
    private final List<TranscriptModel> transcriptModelsAB;
    /** transcripts on the 5' side on contig B or the 3' side of contig A */
    private final List<TranscriptModel> transcriptModelsBA;

    /** transcripts on the 5' side on contig A or the 3' side of contig B */
    private final List<Enhancer> enhancersAB;
    /** transcripts on the 5' side on contig B or the 3' side of contig A */
    private final List<Enhancer> enhancersBA;

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
        this.transcriptModelsAB = new ArrayList<>();
        this.transcriptModelsBA = new ArrayList<>();
        this.enhancersAB = new ArrayList<>();
        this.enhancersBA = new ArrayList<>();
        for (var tmod : transcripts) {
            if (tmod.getChr() == contigA.getId()) {
                // TODO -- we are comparing contig int ids from two different sources.
                if (tmod.getTXRegion().getBeginPos() < positionContigA) {
                    // the transcript is either completely to the left of the breakpoint on contig A
                    // or it straddles the breakpoint
                    this.transcriptModelsAB.add(tmod);
                } else {
                    this.transcriptModelsBA.add(tmod);
                }
            } else if (tmod.getChr() == contigB.getId()) {
                if (tmod.getTXRegion().getBeginPos() < positionContigB) {
                    this.transcriptModelsBA.add(tmod);
                } else {
                    this.transcriptModelsAB.add(tmod);
                }
            } else {
                throw new SvAnnRuntimeException("Could not identify TranscriptModel contig: " + tmod.getChr());
            }
        }
        for (var e : enhancers) {
            if (e.getContig() == contigA) {
                if (e.getStartPosition() < positionContigA) {
                    // the enhancer is either completely to the left of the breakpoint on contig A
                    // or it straddles the breakpoint
                    this.enhancersAB.add(e);
                } else {
                    this.enhancersBA.add(e);
                }
            } else if (e.getContig() == contigB) {
                if (e.getStartPosition() < positionContigB) {
                    // the enhancer is either completely to the left of the breakpoint on contig A
                    // or it straddles the breakpoint
                    this.enhancersBA.add(e);
                } else {
                    this.enhancersAB.add(e);
                }
            }
        }
        this.minPosA = getGenomicMinPosOnContig(contigA, positionContigA);
        this.minPosB = getGenomicMinPosOnContig(contigB, positionContigB);
        this.maxPosA = getGenomicMaxPosOnContig(contigA, positionContigA);
        this.maxPosB = getGenomicMaxPosOnContig(contigB, positionContigB);

    }


    private int getGenomicMinPosOnContig(Contig contig, int pos) {
        List<TranscriptModel> transcripts;
        List<Enhancer> enhancers;
        if (contig == contigA) {
            transcripts = transcriptModelsAB.stream().filter(t -> t.getChr() == contig.getId()).collect(Collectors.toList());
            enhancers = enhancersAB.stream().filter(e -> e.getContig() == contig).collect(Collectors.toList());
        } else {
            transcripts = transcriptModelsBA.stream().filter(t -> t.getChr() == contig.getId()).collect(Collectors.toList());
            enhancers = enhancersBA.stream().filter(e -> e.getContig() == contig).collect(Collectors.toList());
        }
        return getGenomicMinPos(transcripts, enhancers, pos);
    }

    private int getGenomicMaxPosOnContig(Contig contig, int pos) {
        List<TranscriptModel> transcripts;
        List<Enhancer> enhancers;
        if (contig == contigA) {
            transcripts = transcriptModelsBA.stream().filter(t -> t.getChr() == contig.getId()).collect(Collectors.toList());
            enhancers = enhancersBA.stream().filter(e -> e.getContig() == contig).collect(Collectors.toList());
        } else {
            transcripts = transcriptModelsAB.stream().filter(t -> t.getChr() == contig.getId()).collect(Collectors.toList());
            enhancers = enhancersAB.stream().filter(e -> e.getContig() == contig).collect(Collectors.toList());
        }
        return getGenomicMaxPos(transcripts, enhancers, pos);
    }




    @Override
    public void write(Writer writer) throws IOException {
        int starty = 50;
        int y = starty;
//        double start = translateGenomicToSvg(this.insertionCoordinates.getStartPosition());
//        double end = translateGenomicToSvg(this.insertionCoordinates.getEndPosition());
    }
}
