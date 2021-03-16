package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.reference.Exon;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.GenomicRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class GeneSequenceImpactCalculator implements SequenceImpactCalculator<Gene> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneSequenceImpactCalculator.class);

    private static final int DOWNSTREAM_TX_PADDING = 0;
    private static final int INTRONIC_ACCEPTOR_PADDING = 25;
    private static final int INTRONIC_DONOR_PADDING = 6;

    private final int promoterLength;

    private final double geneFactor;

    private final Map<Event, Double> fitnessWithEvent;

    public GeneSequenceImpactCalculator(double geneFactor, int promoterLength) {
        this.promoterLength = promoterLength;
        this.geneFactor = geneFactor;
        this.fitnessWithEvent = Map.of(
                Event.GAP, geneFactor,
                Event.SNV, .85 * geneFactor,
                Event.DUPLICATION, .0,
                Event.INSERTION, .1 * geneFactor,
                Event.DELETION, .0 * geneFactor,
                Event.INVERSION, .0,
                Event.BREAKEND, .0
        );
    }

    @Override
    public double projectImpact(Projection<Gene> projection) {
        Double score = checkPromoter(projection.route().segments(), projection.source().transcripts());

        if (!score.isNaN())
            return score;

        return projection.isIntraSegment()
                ? processIntraSegmentProjection(projection)
                : processInterSegmentProjection(projection);
    }

    @Override
    public double noImpact() {
        return geneFactor;
    }

    private Double checkPromoter(List<Segment> segments, Set<Transcript> transcripts) {
        Set<Segment> nonGapSegments = segments.stream()
                .filter(s -> s.event() != Event.GAP)
                .collect(Collectors.toSet());

        Double score = Double.NaN;
        for (Transcript tx : transcripts) {
            int txStart = tx.start();
            int promoterStart = txStart - promoterLength;
            int promoterEnd = txStart + Coordinates.endDelta(tx.coordinateSystem());

            for (Segment nonGapSegment : nonGapSegments) {
                int segmentStart = nonGapSegment.startOnStrand(tx.strand());
                int segmentEnd = nonGapSegment.endOnStrand(tx.strand());
                if (Coordinates.overlap(tx.coordinateSystem(), promoterStart, promoterEnd,
                        nonGapSegment.coordinateSystem(), segmentStart, segmentEnd)) {
                    Double fitness = fitnessWithEvent.get(nonGapSegment.event());
                    if (score.isNaN() || fitness < score)
                        score = fitness;
                }
            }
        }
        return score;
    }

    private double processIntraSegmentProjection(Projection<Gene> projection) {
        switch (projection.startEvent()) {
            // TODO - plug in haploinsufficiency
            case DELETION:
                // the entire gene is deleted
                return 0.;
            case DUPLICATION:
                // TODO - plug in triplosensitivity
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

    private double processInterSegmentProjection(Projection<Gene> projection) {
        Set<Segment> spannedSegments = projection.spannedSegments();

        return projection.source().transcripts().stream()
                .mapToDouble(tx -> evaluateSegmentsWrtTranscript(spannedSegments, tx))
                .min()
                .orElse(noImpact());
    }

    private double evaluateSegmentsWrtTranscript(Set<Segment> segments, Transcript tx) {
        double score = noImpact();

        if (!tx.isCoding())
            // heuristics shortcut
            return score;

        //noinspection OptionalGetWithoutIsPresent
        GenomicRegion cds = tx.cdsRegion().get();
        int cdsStart = cds.start();
        int cdsEnd = cds.end();

        for (Segment segment : segments) {
            int segmentStart = segment.startOnStrand(tx.strand());
            int segmentEnd = segment.endOnStrand(tx.strand());


            Exon first = tx.exons().get(0);
            int firstExonStart = first.start();
            int firstExonEnd = first.end() + INTRONIC_DONOR_PADDING;
            score = Math.min(evaluateExon(segment.coordinateSystem(), segmentStart, segmentEnd, segment.event(), first.coordinateSystem(), firstExonStart, first.start(), first.end(), firstExonEnd, cdsStart, cdsEnd), score);

            // internal exons
            for (int i = 1; i < tx.exons().size() - 1; i++) {
                Exon exon = tx.exons().get(i);
                int paddedExonStart = exon.start() - INTRONIC_ACCEPTOR_PADDING;
                int paddedExonEnd = exon.end() + INTRONIC_DONOR_PADDING;
                score = Math.min(evaluateExon(segment.coordinateSystem(), segmentStart, segmentEnd, segment.event(), exon.coordinateSystem(), paddedExonStart, exon.start(), exon.end(), paddedExonEnd, cdsStart, cdsEnd), score);
            }


            Exon last = tx.exons().get(tx.exons().size() - 1);
            int lastExonStart = last.start() - INTRONIC_ACCEPTOR_PADDING;
            int lastExonEnd = last.end() + DOWNSTREAM_TX_PADDING;
            score = Math.min(evaluateExon(segment.coordinateSystem(), segmentStart, segmentEnd, segment.event(), last.coordinateSystem(), lastExonStart, last.start(), last.end(), lastExonEnd, cdsStart, cdsEnd), score);
        }

        return score;
    }

    private double evaluateExon(CoordinateSystem segmentCs, int segmentStart, int segmentEnd, Event event, CoordinateSystem exonCs, int spliceStart, int exonStart, int exonEnd, int spliceEnd,
                                int cdsStart, int cdsEnd) {
        if (!Coordinates.overlap(segmentCs, segmentStart, segmentEnd, exonCs, spliceStart, spliceEnd))
            return noImpact();
        if (event.equals(Event.GAP))
            return noImpact();

        boolean affectsCds = Coordinates.overlap(segmentCs, segmentStart, segmentEnd, exonCs, cdsStart, cdsEnd);
        if (!affectsCds) {
            // may be 5UTR or 3UTR
            if (cdsEnd < segmentStart) { // 3UTR
                // TODO - improve as 3'UTR might be in fact multi-exonic
                int utr3length = Coordinates.length(exonCs, exonStart, exonEnd);
                if (utr3length != 0) {
                    double seqImpact = 2. * Coordinates.length(segmentCs, segmentStart, segmentEnd) / utr3length;
                    return Math.max(1 - seqImpact, 0.);
                }
            }
        }
        return fitnessWithEvent.getOrDefault(event, noImpact());
    }
}
