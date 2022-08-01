package org.monarchinitiative.svanna.core.priority.additive.impact;

import org.monarchinitiative.svanna.core.priority.additive.Event;
import org.monarchinitiative.svanna.core.priority.additive.Projection;
import org.monarchinitiative.svanna.core.priority.additive.Segment;
import org.monarchinitiative.sgenes.model.Coding;
import org.monarchinitiative.sgenes.model.Gene;
import org.monarchinitiative.sgenes.model.Transcript;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class GeneSequenceImpactCalculator implements SequenceImpactCalculator<Gene> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneSequenceImpactCalculator.class);
    private static final CoordinateSystem CS = CoordinateSystem.zeroBased();

    // These are fitnesses!
    private static final double INS_SHIFTS_CODING_FRAME = .1;
    private static final double INS_FITS_INTO_CODING_FRAME_IS_INFRAME = .8;
    private static final int INTRONIC_ACCEPTOR_PADDING = 25;
    private static final int INTRONIC_DONOR_PADDING = 6;

    private final double geneFactor;
    private final int promoterLength;
    private final double promoterFitnessGain;

    private final Map<Event, Double> fitnessWithEvent;

    public GeneSequenceImpactCalculator(double geneFactor,
                                        int promoterLength,
                                        double promoterFitnessGain) {
        this.promoterLength = promoterLength;
        this.geneFactor = geneFactor;
        if (promoterFitnessGain > 1.)
            LOGGER.warn("Promoter fitness gain {} cannot be greater than 1. Clipping to 1", promoterFitnessGain);
        this.promoterFitnessGain = Math.min(promoterFitnessGain, 1.);
        this.fitnessWithEvent = Map.of(
                Event.GAP, geneFactor,
                Event.SNV, .85 * geneFactor,
                Event.DUPLICATION, .0,
                Event.INSERTION, .1 * geneFactor,
                Event.DELETION, .0, // * geneFactor,
                Event.INVERSION, .0,
                Event.BREAKEND, .0
        );
    }

    private static double defaultUtrFitness(int segmentLength, int utrLength) {
        // min fitness is attained if >=50% of the UTR length is affected
        double seqImpact = 2. * segmentLength / utrLength;
        return Math.max(1 - seqImpact, 0.);
    }

    private static double insertionUtrFitness(int segmentLength, int utrLength) {
        double impact = (double) segmentLength / utrLength;
        return 1 - Math.min(impact, 1.);
    }

    private static List<PaddedExon> mapToPaddedExons(Transcript tx, int cdsStart, int cdsEnd) {
        // exons are padded to include splice site regions
        if (tx.exons().isEmpty())
            throw new IllegalArgumentException("Transcript with no exons: " + tx.accession());

        if (tx.exons().size() == 1) {
            // unspliced transcripts (1 exon) have no splice regions, hence no padding
            Coordinates first = tx.exons().get(0);
            int start = first.startWithCoordinateSystem(CS);
            int end = first.endWithCoordinateSystem(CS);

            int nCoding = cdsEnd - cdsStart;
            return List.of(PaddedExon.of(start, start, end, end, nCoding));
        }


        List<PaddedExon> exons = new ArrayList<>(tx.exons().size());

        // The first exon
        Coordinates first = tx.exons().get(0);
        int firstExonStart = first.startWithCoordinateSystem(CS);
        int firstExonEnd = first.endWithCoordinateSystem(CS);
        int firstExonPaddedEnd = firstExonEnd + INTRONIC_DONOR_PADDING;

        int nCoding = Coordinates.overlapLength(CS, cdsStart, cdsEnd, first.coordinateSystem(), first.start(), first.end());

        exons.add(PaddedExon.of(firstExonStart, firstExonStart, firstExonEnd, firstExonPaddedEnd, nCoding));

        // The internal exons
        for (Coordinates exon : tx.exons().subList(1, tx.exons().size() - 1)) {
            int exonStart = exon.startWithCoordinateSystem(CS);
            int paddedExonStart = exonStart - INTRONIC_ACCEPTOR_PADDING;
            int exonEnd = exon.endWithCoordinateSystem(CS);
            int paddedExonEnd = exonEnd + INTRONIC_DONOR_PADDING;

            int nCodingInternal = Coordinates.overlapLength(CS, cdsStart, cdsEnd, exon.coordinateSystem(), exon.start(), exon.end());

            exons.add(PaddedExon.of(paddedExonStart, exonStart, exonEnd, paddedExonEnd, nCodingInternal));
        }

        // The last exon
        Coordinates last = tx.exons().get(tx.exons().size() - 1);
        int lastExonStart = last.startWithCoordinateSystem(CS);
        int lastExonPaddedStart = lastExonStart - INTRONIC_ACCEPTOR_PADDING;
        int lastExonEnd = last.endWithCoordinateSystem(CS);

        int nCodingLast = Coordinates.overlapLength(CS, cdsStart, cdsEnd, last.coordinateSystem(), last.start(), last.end());

        exons.add(PaddedExon.of(lastExonPaddedStart, lastExonStart, lastExonEnd, lastExonEnd, nCodingLast));

        return exons;
    }

    @Override
    public double projectImpact(Projection<Gene> projection) {
        List<Transcript> transcripts = projection.source().transcriptStream()
                .collect(Collectors.toList());
        double promoterImpact = checkPromoter(projection.route().segments(), transcripts);

        double geneImpact = projection.isIntraSegment()
                ? processIntraSegmentProjection(projection)
                : processInterSegmentProjection(projection, transcripts);

        return Double.isNaN(promoterImpact)
                ? geneImpact
                : Math.min(geneImpact, promoterImpact);
    }

    @Override
    public double noImpact() {
        return geneFactor;
    }

    private double checkPromoter(Collection<Segment> segments, Iterable<Transcript> transcripts) {
        Iterable<Segment> nonGapSegments = segments.stream()
                .filter(s -> !Event.GAP.equals(s.event()))
                .collect(Collectors.toList());

        double score = Double.NaN;
        for (Transcript tx : transcripts) {
            int txStart = tx.start();
            int promoterStart = Math.max(txStart - promoterLength, 0); // Let's not allow negative start coordinate.
            int promoterEnd = txStart + Coordinates.endDelta(tx.coordinateSystem());

            for (Segment nonGapSegment : nonGapSegments) {
                int segmentStart = nonGapSegment.startOnStrand(tx.strand());
                int segmentEnd = nonGapSegment.endOnStrand(tx.strand());
                if (Coordinates.overlap(tx.coordinateSystem(), promoterStart, promoterEnd,
                        nonGapSegment.coordinateSystem(), segmentStart, segmentEnd)) {
                    double fitness;
                    if (Event.INVERSION.equals(nonGapSegment.event())) {
                        if (Coordinates.aContainsB(CS, segmentStart, segmentEnd, CS, promoterStart, promoterEnd))
                            // Inversion of an entire promoter is not considered deleterious.
                            fitness = 1;
                        else
                            // The promoter is disrupted by the inversion boundary, hence deleterious.
                            fitness = fitnessWithEvent.get(Event.INVERSION);
                    } else {
                        // All other events are scored using default scoring scheme.
                        fitness = fitnessWithEvent.get(nonGapSegment.event());
                    }

                    // In theory, `fitness + geneFactor * promoterFitnessGain` can be above 1, which does not make sense.
                    // Let's not allow that to happen.
                    double maxAllowedFitness = Math.min(fitness + geneFactor * promoterFitnessGain, 1.);
                    fitness = Math.min(maxAllowedFitness, geneFactor);

                    score = Double.isNaN(score)
                            ? fitness
                            : Math.min(fitness, score);
                }
            }
        }
        return score;
    }

    private double processIntraSegmentProjection(Projection<Gene> projection) {
        // the entire gene is spanned by an event (DELETION, INVERSION, DUPLICATION...)
        switch (projection.startEvent()) {
            case DELETION:
                // the entire gene is deleted
                return 0.;
            case DUPLICATION:
                // the entire gene is duplicated
                return 2 * noImpact();
            case BREAKEND:
            case SNV:
            case INSERTION:
                // should not happen since these are events with length 0
                LOGGER.warn("Gene is unexpectedly located within a {}. " +
                                "This should not have happened since length of {} on reference contig should be too small to encompass a gene",
                        projection.startEvent(), projection.startEvent());
                return noImpact();
            default:
                LOGGER.warn("Unable to process unknown impact");
            case GAP: // no impact
            case INVERSION: // the entire gene is inverted, this should not be an issue
                return noImpact();
        }
    }

    private double processInterSegmentProjection(Projection<Gene> projection, Collection<Transcript> transcripts) {
        Set<Segment> causalSegments = projection.spannedSegments().stream()
                .filter(s -> !s.event().equals(Event.GAP))
                .collect(Collectors.toUnmodifiableSet());

        double score = noImpact();
        for (Transcript tx : transcripts) {
            double txScore = evaluateSegmentsWrtTranscript(causalSegments, tx);
            score = Math.min(score, txScore);
        }
        return score;
    }

    private double evaluateSegmentsWrtTranscript(Set<Segment> segments, Transcript tx) {
        double score = noImpact();

        if (!(tx instanceof Coding))
            // heuristics shortcut for noncoding transcript
            return score;

        Coding ctx = (Coding) tx;
        UtrData utrData = UtrData.of(tx.startWithCoordinateSystem(CS), ctx.codingStartWithCoordinateSystem(CS),
                ctx.codingEndWithCoordinateSystem(CS), tx.endWithCoordinateSystem(CS));

        for (Segment segment : segments) {
            double segmentScore;
            //noinspection SwitchStatementWithTooFewBranches
            switch (segment.event()) {
                case INSERTION:
                    segmentScore = scoreInsertionSegment(segment, tx, utrData);
                    break;
                default:
                    segmentScore = scoreDefaultSegment(segment, tx, utrData);
                    break;
            }
            score = Math.min(segmentScore, score);
        }

        return score;
    }

    double scoreInsertionSegment(Segment segment, Transcript tx, UtrData utrData) {
        int insLengthOnContig = segment.endWithCoordinateSystem(CS) - segment.startWithCoordinateSystem(CS);
        if (insLengthOnContig != 0) {
            LOGGER.warn("Bad insertion with nonzero length {}", insLengthOnContig);
            return Double.NaN;
        }

        // segment start and end of an empty region are the same numbers in both half-open coordinate systems
        int segmentPos = segment.startOnStrandWithCoordinateSystem(tx.strand(), CS);

        double score = noImpact();

        List<PaddedExon> paddedExons = mapToPaddedExons(tx, utrData.cdsStart(), utrData.cdsEnd());

        int nCodingBasesInPreviousExons = 0;
        for (PaddedExon exon : paddedExons) {
            if (Coordinates.aContainsB(CS, exon.paddedStart(), exon.paddedEnd(),
                    segment.coordinateSystem(), segment.startOnStrand(tx.strand()), segment.endOnStrand(tx.strand()))) {
                // is the insertion in UTR?
                if (segmentPos <= utrData.cdsStart()) {
                    // 5'UTR
                    score = Math.min(insertionUtrFitness(segment.length(), utrData.fiveUtrLength()), score);
                } else if (utrData.cdsEnd() < segmentPos) {
                    // 3'UTR
                    score = Math.min(insertionUtrFitness(segment.length(), utrData.threeUtrLength()), score);
                } else {
                    // coding region
                    int nCurrentCodingBases = segmentPos - Math.max(utrData.cdsStart(), exon.start());
                    int nTotalCodingBases = nCodingBasesInPreviousExons + nCurrentCodingBases;

                    boolean fitsIntoCodingFrame = nTotalCodingBases % 3 == 0;
                    boolean insertionIsInFrame = segment.length() % 3 == 0;

                    if (fitsIntoCodingFrame && insertionIsInFrame)
                        score = Math.min(INS_FITS_INTO_CODING_FRAME_IS_INFRAME, score);
                    else
                        score = Math.min(INS_SHIFTS_CODING_FRAME, score);
                }
                break; // insertion has length 0 and does not overlap with multiple exons
            } else {
                nCodingBasesInPreviousExons += exon.nCodingBases();
            }
        }

        return score;
    }

    double scoreDefaultSegment(Segment segment, Transcript tx, UtrData utrData) {
        double score = noImpact();
        if (segment.event() == Event.GAP)
            return score;

        int segmentStart = segment.startOnStrandWithCoordinateSystem(tx.strand(), CS);
        int segmentEnd = segment.endOnStrandWithCoordinateSystem(tx.strand(), CS);

        List<PaddedExon> paddedExons = mapToPaddedExons(tx, utrData.cdsStart(), utrData.cdsEnd());
        int nCodingBasesInPreviousExons = 0;
        boolean isFirstExonOfTranscript = true;
        for (PaddedExon exon : paddedExons) {
            if (Coordinates.overlap(CS, exon.paddedStart(), exon.paddedEnd(), CS, segmentStart, segmentEnd)) {
                // we evaluate the segment exon-by-exon, and we take the most severe score.
                double exonScore = evaluateExon(segmentStart, segmentEnd, segment.event(), exon, utrData, isFirstExonOfTranscript, nCodingBasesInPreviousExons);
                score = Math.min(exonScore, score);
            }

            if (isFirstExonOfTranscript)
                isFirstExonOfTranscript = false;
            nCodingBasesInPreviousExons += exon.nCodingBases();
        }

        return score;
    }

    private double evaluateExon(int segmentStart,
                                int segmentEnd,
                                Event event,
                                PaddedExon exon,
                                UtrData utrData,
                                boolean isFirstExonOfTranscript,
                                int nCodingBasesInPreviousExons) {
        // From now on the segment overlaps with the exon
        boolean affectsCds = Coordinates.overlap(CS, segmentStart, segmentEnd, CS, utrData.cdsStart(), utrData.cdsEnd());

        // Check if segment overlaps with transcription start site
        if (isFirstExonOfTranscript) {
            boolean segmentContainsExonStart = segmentStart <= exon.start() && exon.start() < segmentEnd;
            if (segmentContainsExonStart)
                return fitnessWithEvent.getOrDefault(event, noImpact());
        }

        if (affectsCds) {
            // Segment affects the coding sequence
            boolean eventFitsIntoFrame = (nCodingBasesInPreviousExons + segmentStart - exon.start()) % 3 == 0;
            boolean eventInvolvesMultipleOfThree = (segmentEnd - segmentStart) % 3 == 0;

            switch (event) {
                case DELETION:
                case DUPLICATION:
                    // Is the deletion/duplication in-frame or out of frame?
                    return (eventFitsIntoFrame && eventInvolvesMultipleOfThree)
                            ? .2 * geneFactor
                            : fitnessWithEvent.getOrDefault(event, noImpact());
                case INSERTION:
                    throw new IllegalArgumentException("Insertion segment should not be handled as a default segment!");
                default:
                    return fitnessWithEvent.getOrDefault(event, noImpact());

            }
        } else {
            if (segmentEnd <= utrData.cdsStart()) { // 5UTR
                if (utrData.fiveUtrLength() == 0) {
                    LOGGER.warn("5'UTR was 0bp long!");
                    return noImpact();
                }
                return defaultUtrFitness(segmentEnd - segmentStart, utrData.fiveUtrLength());
            } else if (utrData.cdsEnd() <= segmentStart) { // 3UTR
                if (utrData.threeUtrLength() == 0) {
                    LOGGER.warn("3'UTR was 0bp long!");
                    return noImpact();
                }
                return defaultUtrFitness(segmentEnd - segmentStart, utrData.threeUtrLength());
            } else {
                // Something fishy! The segment neither affects CDS nor it overlaps with 5' or 3' UTRs! Complain!
                throw new IllegalArgumentException("Segment " + segmentStart + '-' + segmentEnd + " should overlap with 5UTR or 3UTR, but it does not!");
            }
        }
    }

}
