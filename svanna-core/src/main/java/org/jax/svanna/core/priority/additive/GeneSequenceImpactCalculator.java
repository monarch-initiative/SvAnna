package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.reference.Exon;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.Coordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class GeneSequenceImpactCalculator implements SequenceImpactCalculator<Gene> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneSequenceImpactCalculator.class);

    private static final int UPSTREAM_PROMOTOR_PADDING = 500;
    private static final int DOWNSTREAM_TX_PADDING = 0;
    private static final int INTRONIC_ACCEPTOR_PADDING = 25;
    private static final int INTRONIC_DONOR_PADDING = 6;

    private final double geneFactor;

    private final Map<Event, Double> fitnessWithEvent;

    public GeneSequenceImpactCalculator(double geneFactor) {
        this.geneFactor = geneFactor;
        this.fitnessWithEvent = Map.of(
                Event.GAP, geneFactor,
                Event.SNV, .85 * geneFactor,
                Event.DUPLICATION, .3 * geneFactor,
                Event.INSERTION, .1 * geneFactor,
                Event.DELETION, .1 * geneFactor,
                Event.INVERSION, .0,
                Event.BREAKEND, .0
        );
    }

    @Override
    public double projectImpact(Projection<Gene> projection) {
        return projection.isIntraSegment()
                ? processIntraSegmentProjection(projection)
                : processInterSegmentProjection(projection);
    }

    @Override
    public double noImpact() {
        return geneFactor;
    }

    private double processInterSegmentProjection(Projection<Gene> projection) {
        Set<Segment> spannedSegments = projection.spannedSegments();

        return projection.source().transcripts().stream()
                .mapToDouble(tx -> evaluateSegmentsWrtTranscript(spannedSegments, tx))
                .min()
                .orElse(noImpact());
    }

    private double processIntraSegmentProjection(Projection<Gene> projection) {
        switch (projection.startEvent()) {
            case DELETION:
                // the entire gene region is deleted
                return 0.;
            case DUPLICATION:
                // the entire gene is duplicated
                return 2 * noImpact();
            case BREAKEND:
            case SNV:
            case INSERTION:
                // should not happen since these are events with length 0
                LogUtils.logWarn(LOGGER, "Gene is unexpectedly located within a {}. " +
                        "This should not have happened since length of {} on reference contig should be too small to encompass a gene", projection.startEvent(), projection.startEvent());
                return noImpact();
            default:
                LogUtils.logWarn(LOGGER, "Unable to process unknown impact");
            case GAP: // no impact
            case INVERSION: // the entire gene is inverted, this should not be an issue
                return noImpact();
        }
    }

    private double evaluateSegmentsWrtTranscript(Set<Segment> segments, Transcript tx) {
        double score = noImpact();
        for (Segment segment : segments) {
            int segmentStart = segment.startOnStrand(tx.strand());
            int segmentEnd = segment.endOnStrand(tx.strand());


            Exon first = tx.exons().get(0);
            int promoterAndExonStart = first.start() - UPSTREAM_PROMOTOR_PADDING;
            int firstExonEnd = first.end() + INTRONIC_DONOR_PADDING;
            if (Coordinates.overlap(segment.coordinateSystem(), segmentStart, segmentEnd, first.coordinateSystem(), promoterAndExonStart, firstExonEnd))
                score = Math.min(fitnessWithEvent.getOrDefault(segment.event(), noImpact()), score);


            // internal exons
            for (int i = 1; i < tx.exons().size() - 1; i++) {
                Exon exon = tx.exons().get(i);
                int paddedExonStart = exon.start() - INTRONIC_ACCEPTOR_PADDING;
                int paddedExonEnd = exon.end() + INTRONIC_DONOR_PADDING;
                if (Coordinates.overlap(segment.coordinateSystem(), segmentStart, segmentEnd,
                        exon.coordinateSystem(), paddedExonStart, paddedExonEnd)) {
                    score = Math.min(fitnessWithEvent.getOrDefault(segment.event(), noImpact()), score);
                }
            }


            Exon last = tx.exons().get(tx.exons().size() - 1);
            int lastExonStart = last.start() - INTRONIC_ACCEPTOR_PADDING;
            int lastExon = first.end() + DOWNSTREAM_TX_PADDING;
            if (Coordinates.overlap(segment.coordinateSystem(), segmentStart, segmentEnd, last.coordinateSystem(), lastExonStart, lastExon))
                score = Math.min(fitnessWithEvent.getOrDefault(segment.event(), noImpact()), score);
        }

        return score;
    }
}
