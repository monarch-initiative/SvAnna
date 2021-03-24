package org.jax.svanna.core.priority.additive.impact;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.priority.additive.Event;
import org.jax.svanna.core.priority.additive.Projection;
import org.jax.svanna.core.priority.additive.Segment;
import org.jax.svanna.core.reference.CodingTranscript;
import org.jax.svanna.core.reference.Exon;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode") // TODO - revise
public class GeneSequenceImpactCalculator implements SequenceImpactCalculator<Gene> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneSequenceImpactCalculator.class);

    // These are fitnesses!
    private static final double INS_DOES_NOT_FIT_INTO_CODING_FRAME = .1;
    private static final double INS_FITS_INTO_CODING_FRAME_IS_OUT_OF_FRAME = .5;
    private static final double INS_FITS_INTO_CODING_FRAME_IS_INFRAME = .8;

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
        // the entire gene is spanned by an event (DELETION, INVERSION, DUPLICATION...
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
        Set<Segment> causalSegments = projection.spannedSegments().stream()
                .filter(s -> !s.event().equals(Event.GAP))
                .collect(Collectors.toSet());

        double score = noImpact();
        for (Transcript tx : projection.source().transcripts()) {
            double txScore = evaluateSegmentsWrtTranscript(causalSegments, tx);
            score = Math.min(score, txScore);
        }
        return score;
    }

    private double evaluateSegmentsWrtTranscript(Set<Segment> segments, Transcript tx) {
        double score = noImpact();

        if (!(tx instanceof CodingTranscript))
            // heuristics shortcut for noncoding transcript
            return score;

        CodingTranscript ctx = (CodingTranscript) tx;
        for (Segment segment : segments) {
            double segmentScore;
            //noinspection SwitchStatementWithTooFewBranches
            switch (segment.event()) {
                case INSERTION:
                    segmentScore = scoreInsertionSegment(segment, ctx);
                    break;
                default:
                    segmentScore = scoreDefaultSegment(segment, ctx);
                    break;
            }
            score = Math.min(segmentScore, score);
        }

        return score;
    }

    double scoreInsertionSegment(Segment segment, CodingTranscript tx) {
        int insLengthOnContig = segment.endWithCoordinateSystem(CoordinateSystem.zeroBased()) - segment.startWithCoordinateSystem(CoordinateSystem.zeroBased());
        if (insLengthOnContig != 0) {
            LogUtils.logWarn(LOGGER, "Bad insertion with nonzero length {}", insLengthOnContig);
            return Double.NaN;
        }

        int cdsStart = tx.codingStartWithCoordinateSystem(CoordinateSystem.zeroBased());
        int cdsEnd = tx.codingEndWithCoordinateSystem(CoordinateSystem.zeroBased());

        // segment start and end are the same number in both half-open coordinate systems
        int segmentPos = segment.startWithCoordinateSystem(CoordinateSystem.zeroBased());

        double score = noImpact();

        List<PaddedExon> paddedExons = mapToPaddedExons(tx);

        int nCodingBasesInPreviousExons = 0;
        for (PaddedExon exon : paddedExons) {
            if (Coordinates.aContainsB(CoordinateSystem.zeroBased(), exon.paddedStart(), exon.paddedEnd(),
                    segment.coordinateSystem(), segment.start(), segment.end())) {
                // is the insertion in UTR?
                if (segmentPos <= cdsStart) {
                    // 5'UTR
                    score = Math.min(insertionUtrFitness(segment, tx.fivePrimeUtrLength()), score);
                } else if (cdsEnd < segmentPos) {
                    // 3'UTR
                    score = Math.min(insertionUtrFitness(segment, tx.threePrimeUtrLength()), score);
                } else {
                    // coding region
                    int nCurrentCodingBases = segmentPos - Math.max(cdsStart, exon.start());
                    int nTotalCodingBases = nCodingBasesInPreviousExons + nCurrentCodingBases;
                    boolean fitsIntoCodingFrame = nTotalCodingBases % 3 == 0;
                    if (fitsIntoCodingFrame) {
                        boolean isInFrame = segment.length() % 3 == 0;
                        score = isInFrame
                                ? Math.min(INS_FITS_INTO_CODING_FRAME_IS_INFRAME, score)
                                : Math.min(INS_FITS_INTO_CODING_FRAME_IS_OUT_OF_FRAME, score);
                    } else
                        score = Math.min(INS_DOES_NOT_FIT_INTO_CODING_FRAME, score);
                }
            } else {
                nCodingBasesInPreviousExons += exon.nCodingBases();
            }
        }

        return score;
    }

    double scoreDefaultSegment(Segment segment, CodingTranscript tx) {
        int cdsStart = tx.codingStartWithCoordinateSystem(CoordinateSystem.zeroBased());
        int cdsEnd = tx.codingEndWithCoordinateSystem(CoordinateSystem.zeroBased());

        int segmentStart = segment.startOnStrandWithCoordinateSystem(tx.strand(), CoordinateSystem.zeroBased());
        int segmentEnd = segment.endOnStrandWithCoordinateSystem(tx.strand(), CoordinateSystem.zeroBased());

        double score = noImpact();

        List<PaddedExon> paddedExons = mapToPaddedExons(tx);
        for (PaddedExon paddedExon : paddedExons) {
            double exonScore = evaluateExon(segmentStart, segmentEnd, segment.event(), paddedExon, cdsStart, cdsEnd);
            score = Math.min(exonScore, score);
        }

        return score;
    }

    private double evaluateExon(int segmentStart, int segmentEnd, Event event, PaddedExon exon, int cdsStart, int cdsEnd) {
        if (!Coordinates.overlap(CoordinateSystem.zeroBased(), segmentStart, segmentEnd, CoordinateSystem.zeroBased(), exon.paddedStart(), exon.paddedEnd()))
            return noImpact();
        if (event.equals(Event.GAP))
            return noImpact();

        boolean affectsCds = Coordinates.overlap(CoordinateSystem.zeroBased(), segmentStart, segmentEnd, CoordinateSystem.zeroBased(), cdsStart, cdsEnd);
        if (!affectsCds) {
            // may be 5UTR or 3UTR
            if (cdsEnd < segmentStart) { // 3UTR
                // TODO - improve as 3'UTR might be in fact multi-exonic
                int utr3length = Coordinates.length(CoordinateSystem.zeroBased(), exon.start(), exon.end());
                if (utr3length != 0) {
                    double seqImpact = 2. * Coordinates.length(CoordinateSystem.zeroBased(), segmentStart, segmentEnd) / utr3length;
                    return Math.max(1 - seqImpact, 0.);
                }
            }
        }
        return fitnessWithEvent.getOrDefault(event, noImpact());
    }

    private static double insertionUtrFitness(Segment segment, int utrLength) {
        double impact = (double) segment.length() / utrLength;
        return 1 - Math.min(impact, 1.);
    }

    private static List<PaddedExon> mapToPaddedExons(CodingTranscript tx) {
        if (tx.exons().isEmpty()) {
            throw new IllegalArgumentException("Transcript with no exons: " + tx.accessionId());
        }
        int cdsStart = tx.codingEndWithCoordinateSystem(CoordinateSystem.zeroBased());
        int cdsEnd = tx.codingStartWithCoordinateSystem(CoordinateSystem.zeroBased());

        if (tx.exons().size() == 1) {
            Exon first = tx.exons().get(0);
            int start = first.startWithCoordinateSystem(CoordinateSystem.zeroBased());
            int end = first.endWithCoordinateSystem(CoordinateSystem.zeroBased());
            int endPadded = first.endWithCoordinateSystem(CoordinateSystem.zeroBased());

            int nCoding = cdsStart - cdsEnd;
            return List.of(PaddedExon.of(start, start, end, endPadded, nCoding));
        }


        List<PaddedExon> exons = new ArrayList<>(tx.exons().size());

        Exon first = tx.exons().get(0);
        int firstExonStart = first.startWithCoordinateSystem(CoordinateSystem.zeroBased());
        int firstExonEnd = first.endWithCoordinateSystem(CoordinateSystem.zeroBased());
        int firstExonPaddedEnd = firstExonEnd + INTRONIC_DONOR_PADDING;

        int nCoding = Coordinates.overlapLength(CoordinateSystem.zeroBased(), cdsStart, cdsEnd,
                first.coordinateSystem(), first.start(), first.end());

        exons.add(PaddedExon.of(firstExonStart, firstExonStart, firstExonEnd, firstExonPaddedEnd, nCoding));

        // internal exons
        for (Exon exon : tx.exons().subList(1, tx.exons().size() - 1)) {
            int exonStart = exon.startWithCoordinateSystem(CoordinateSystem.zeroBased());
            int paddedExonStart = exonStart - INTRONIC_ACCEPTOR_PADDING;
            int exonEnd = exon.endWithCoordinateSystem(CoordinateSystem.zeroBased());
            int paddedExonEnd = exonEnd + INTRONIC_DONOR_PADDING;

            int nCodingInternal = Coordinates.overlapLength(CoordinateSystem.zeroBased(), cdsStart, cdsEnd,
                    exon.coordinateSystem(), exon.start(), exon.end());

            exons.add(PaddedExon.of(paddedExonStart, exonStart, exonEnd, paddedExonEnd, nCodingInternal));
        }

        Exon last = tx.exons().get(tx.exons().size() - 1);
        int lastExonStart = last.startWithCoordinateSystem(CoordinateSystem.zeroBased());
        int lastExonPaddedStart = lastExonStart - INTRONIC_ACCEPTOR_PADDING;
        int lastExonEnd = last.endWithCoordinateSystem(CoordinateSystem.zeroBased());

        int nCodingLast = Coordinates.overlapLength(CoordinateSystem.zeroBased(), cdsStart, cdsEnd,
                last.coordinateSystem(), last.start(), last.end());

        exons.add(PaddedExon.of(lastExonPaddedStart, lastExonStart, lastExonEnd, lastExonEnd, nCodingLast));

        return exons;
    }

}
