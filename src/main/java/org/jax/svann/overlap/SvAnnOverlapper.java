package org.jax.svann.overlap;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.transcripts.SvAnnTxModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jax.svann.overlap.OverlapType.*;

/**
 * This class determines the kind and degree of overlap of a structural variant with transcript features.
 */
public class SvAnnOverlapper implements Overlapper {


    /*
     * Implementation notes
     * - there is a method {@link #getOverlapList(GenomeInterval, IntervalArray.QueryResult)} that should be used to get
     *   overlaps for any structural variant.
     * - variant must be flipped to transcript's strand before calculating any overlaps and distances. coordinates of
     *   the most SV events are on FWD strand by default.
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(SvAnnOverlapper.class);

    /**
     * Map where key corresponds to {@link Contig#getId()} and the value contains interval array with the
     * transcripts.
     */
    private final Map<Integer, IntervalArray<SvAnnTxModel>> intervalArrayMap;

    public SvAnnOverlapper(Map<Integer, IntervalArray<SvAnnTxModel>> intervalArrayMap) {
        this.intervalArrayMap = intervalArrayMap;
    }

    private static Overlap getOverlapForTranscript(GenomicRegion event, SvAnnTxModel tx) {
        GenomicRegion eventOnStrand = event.withStrand(tx.getStrand());
        // get the closest distance
        int startDistance = eventOnStrand.differenceTo(tx.getStart());
        int endDistance = eventOnStrand.differenceTo(tx.getEnd());
        int distance = Math.abs(startDistance) < Math.abs(endDistance)
                ? startDistance
                : endDistance;
        // If we get here, we know that the txLt does not overlap with the event.
        // If the distance is positive, then the event is upstream from the tx.
        // Otherwise, the event is downstream wrt. the tx.
        OverlapType type;
        if (distance > 0) {
            // event is upstream
            if (distance <= 500) {
                type = UPSTREAM_GENE_VARIANT_500B;
            } else if (distance <= 2_000) {
                type = UPSTREAM_GENE_VARIANT_2KB;
            } else if (distance <= 5_000) {
                type = UPSTREAM_GENE_VARIANT_5KB;
            } else if (distance <= 500_000) {
                type = UPSTREAM_GENE_VARIANT_500KB;
            } else {
                type = UPSTREAM_GENE_VARIANT;
            }
        } else {
            // event is downstream from the tx
            if (distance >= -500) {
                type = DOWNSTREAM_GENE_VARIANT_500B;
            } else if (distance >= -2_000) {
                type = DOWNSTREAM_GENE_VARIANT_2KB;
            } else if (distance >= -5_000) {
                type = DOWNSTREAM_GENE_VARIANT_5KB;
            } else if (distance >= -500_000) {
                type = DOWNSTREAM_GENE_VARIANT_500KB;
            } else {
                type = DOWNSTREAM_GENE_VARIANT;
            }
        }

        OverlapDistance overlapDistance = OverlapDistance.fromDownstreamFlankingGene(distance, tx.getGeneSymbol());
        return new Overlap(type, tx, overlapDistance);
    }

    /**
     * We check for overlap in three ways. The structural variant has an interval (b,e). b or e can
     * be contained within an exon. Alternatively, the entire exon can be contained within (b,e). Note
     * that if we call this method then
     *
     * @param tx    A transcript
     * @param event a structural variant interval
     * @return object representing the number of the first and last affected exon
     */
    private static ExonPair getAffectedExons(SvAnnTxModel tx, GenomicRegion event) {
        Optional<Integer> firstAffectedExonNumber = Optional.empty();
        Optional<Integer> lastAffectedExonNumber = Optional.empty();
        List<GenomicRegion> exons = tx.getExonRegions();
        GenomicPosition svStartPos = event.getStart();
        GenomicPosition svEndPos = event.getEnd();
        boolean[] affected = new boolean[exons.size()]; // initializes to false
        for (int i = 0; i < exons.size(); i++) {
            GenomicRegion exon = exons.get(i);
            if (exon.contains(svStartPos)) {
                affected[i] = true;
            }
            if (exon.contains(svEndPos)) {
                affected[i] = true;
            }
            if (event.contains(exon)) {
                affected[i] = true;
            }
        }
        // -1 is a code for not applicable
        // we may encounter transcripts where the exons do not overlap
        // in this case, first and last will not be changed
        // the ExonPair object will treat first=last=-1 as a signal that there
        // is no overlap.
        int first = -1;
        int last = -1;
        for (int i = 0; i < affected.length; i++) {
            if (first < 0 && affected[i]) {
                first = i + 1;
                last = first;
            } else if (first > 0 && affected[i]) {
                last = i + 1;
            }
        }
        return new ExonPair(first, last);
    }

    /**
     * By assumption, if we get here we have previously determined that the SV is located within an intron
     * of the transcript. If the method is called for an SV that is only partially in the intron, it can
     * return incorrect results. It does not check this.
     *
     * @param tx transcript
     * @return intron distance summary
     */
    private static IntronDistance getIntronNumber(SvAnnTxModel tx, GenomicRegion event) {
        List<GenomicRegion> exons = tx.getExonRegions();

        for (int i = 0; i < exons.size() - 1; i++) {
            // current exon end
            GenomicPosition intronStart = exons.get(i).getEnd();
            // next exon start
            GenomicPosition intronEnd = exons.get(i + 1).getStart();

            if (intronStart.isUpstreamOf(event.getStart()) && intronEnd.isDownstreamOf(event.getEnd())) {
                // we start intron numbering at 1
                int intronNumber = i + 1;
                int up = event.getStart().distanceTo(intronStart);
                int down = event.getEnd().distanceTo(intronEnd);
                return new IntronDistance(intronNumber, up, down);
            }

        }
        throw new SvAnnRuntimeException("Could not find intron number");
    }

    @Override
    public List<Overlap> getOverlapList(SequenceRearrangement rearrangement) {
        switch (rearrangement.getType()) {
            case DELETION:
                return getDeletionOverlaps(rearrangement);
            case INSERTION:
                return getInsertionOverlaps(rearrangement);
            case INVERSION:
                return getInversionOverlaps(rearrangement);
            case TRANSLOCATION:
                return getTranslocationOverlaps(rearrangement);
            default:
                LOGGER.warn("Getting overlaps for `{}` is not yet supported", rearrangement.getType());
                return List.of();
        }
    }

    private List<Overlap> getDeletionOverlaps(SequenceRearrangement deletion) {
        List<Adjacency> adjacencies = deletion.getAdjacencies();
        if (adjacencies.size() != 1) {
            LOGGER.warn("Malformed deletion adjacency list with size {}!=1", adjacencies.size());
            return List.of();
        }

        // the adjacency must contain breakends located on the same contigs & strands
        Adjacency adjacency = adjacencies.get(0);
        if (!adjacency.isIntrachromosomal() || !adjacency.getStart().getStrand().equals(adjacency.getEnd().getStrand())) {
            LOGGER.warn("Malformed deletion adjacency, either not intrachromosomal or not on the same strand");
            return List.of();
        }

        // let's construct the deleted region
        StandardGenomicRegion deletedRegion = StandardGenomicRegion.precise(adjacency.getStart().getContig(), adjacency.getStartPosition(), adjacency.getEndPosition(), adjacency.getStrand());

        // get the overlapping transcripts. Note that the interval tree contains coordinates on FWD strand even for txs
        // located on REV strand.
        StandardGenomicRegion deletedRegionOnFwd = deletedRegion.withStrand(Strand.FWD);
        IntervalArray<SvAnnTxModel> contigIArray = intervalArrayMap.get(adjacency.getStartContigId());
        IntervalArray<SvAnnTxModel>.QueryResult queryResult = contigIArray.findOverlappingWithInterval(deletedRegionOnFwd.getStartPosition(), deletedRegionOnFwd.getEndPosition());

        return parseIntrachromosomalEventQueryResult(deletedRegion, queryResult);
    }


    private List<Overlap> getInsertionOverlaps(SequenceRearrangement insertion) {
        List<Adjacency> adjacencies = insertion.getAdjacencies();
        if (adjacencies.size() != 2) {
            LOGGER.warn("Malformed insertion adjacency list with size {}!=2", adjacencies.size());
            return List.of();
        }

        // the first and last breakend of the insertion must be located on the same contigs & strands
        Breakend leftmostBreakend = insertion.getLeftmostBreakend();
        Breakend rightmostBreakend = insertion.getRightmostBreakend();
        if (leftmostBreakend.getContigId() != rightmostBreakend.getContigId() || !leftmostBreakend.getStrand().equals(rightmostBreakend.getStrand())) {
            LOGGER.warn("Malformed insertion adjacencies, either on different contigs or on different strands");
            return List.of();
        }

        // now let's construct the region affected by the insertion
        StandardGenomicRegion insertionRegion = StandardGenomicRegion.precise(
                leftmostBreakend.getContig(),
                leftmostBreakend.getPosition(),
                rightmostBreakend.getPosition(),
                leftmostBreakend.getStrand());

        // get the overlapping transcripts. Note that the interval tree contains coordinates on FWD strand even for txs
        // located on REV strand.
        StandardGenomicRegion insertionRegionOnFwd = insertionRegion.withStrand(Strand.FWD);
        IntervalArray<SvAnnTxModel> contigIArray = intervalArrayMap.get(insertionRegion.getStartContigId());
        IntervalArray<SvAnnTxModel>.QueryResult queryResult = contigIArray.findOverlappingWithInterval(insertionRegionOnFwd.getStartPosition(), insertionRegionOnFwd.getEndPosition());

        return parseIntrachromosomalEventQueryResult(insertionRegion, queryResult);
    }

    private List<Overlap> getInversionOverlaps(SequenceRearrangement inversion) {
        List<Adjacency> adjacencies = inversion.getAdjacencies();
        if (adjacencies.size() != 2) {
            LOGGER.warn("Malformed inversion adjacency list with size {}!=2", adjacencies.size());
            return List.of();
        }

        // the breakends describing the inverted regions must be on the same contig & strand
        Breakend outerLeft = inversion.getLeftmostBreakend();
        Breakend outerRight = inversion.getRightmostBreakend();

        boolean allContigsAreTheSame = outerLeft.getContigId() == outerRight.getContigId();
        boolean allStrandsMatch = outerLeft.getStrand().equals(outerRight.getStrand());

        if (!allContigsAreTheSame || !allStrandsMatch) {
            LOGGER.warn("Malformed inversion adjacencies, either on different contigs or on different strands: {}", inversion);
            return List.of();
        }

        // let's create the outer region and the inverted region
        StandardGenomicRegion outerRegion = StandardGenomicRegion.precise(
                outerLeft.getContig(),
                outerLeft.getPosition(),
                outerRight.getPosition(),
                outerLeft.getStrand());

        GenomicRegion outerOnFwd = outerRegion.withStrand(Strand.FWD);
        IntervalArray<SvAnnTxModel>.QueryResult queryResult = intervalArrayMap.get(outerOnFwd.getContigId()).findOverlappingWithInterval(outerOnFwd.getStartPosition(), outerOnFwd.getEndPosition());

        return parseIntrachromosomalEventQueryResult(outerRegion, queryResult);
    }


    private List<Overlap> getTranslocationOverlaps(SequenceRearrangement translocation) {
        return List.of();
    }

    /**
     * If we get here, we know that the SV event is intrachromosomal.
     *
     * @param event   SV event
     * @param qresult result with transcript features
     * @return overlap list
     */
    private List<Overlap> parseIntrachromosomalEventQueryResult(GenomicRegion event,
                                                                IntervalArray<SvAnnTxModel>.QueryResult qresult) {
        List<Overlap> overlaps = new ArrayList<>();
        if (qresult.getEntries().isEmpty()) {
            return intergenic(event, qresult);
        }
        // if we get here, then we overlap with one or more genes
        List<SvAnnTxModel> overlappingTranscripts = qresult.getEntries();
        for (var tx : overlappingTranscripts) {
            if (event.contains(tx)) {
                // the transcript is completely contained in the SV
                String msg = String.format("%s/%s", tx.getGeneSymbol(), tx.getAccession());
                Overlap overlap = new Overlap(TRANSCRIPT_CONTAINED_IN_SV, tx, OverlapDistance.fromContainedIn(), msg);
                overlaps.add(overlap);
                continue;
            }
            if (!tx.contains(event.getStart()) && !tx.contains(event.getEnd())) {
                LOGGER.error("Warning, transcript model ({};{}) retrieved that does not overlap (chr{}:{}-{}({})): ",
                        tx.getGeneSymbol(), tx.getAccession(), event.getContig().getPrimaryName(), event.getStartPosition(), event.getEndPosition(), event.getStrand());
                // TODO I observed this once, it should never happen and may be a Jannovar bug or have some other cause
            }
            // TODO if the above bug no longer occurs, make a regular if/else with the above
            Overlap overlap = genic(tx, event);
            overlaps.add(overlap);
        }
        if (overlaps.isEmpty()) {
            LOGGER.error("Could not find any overlaps with this query result: {}", qresult);
            throw new SvAnnRuntimeException("Empty overlap list");
        }
        return overlaps;
    }

    /**
     * Calculate overlap for a non-coding transcript. By assumption, if we get here then we have already
     * determined that the SV overlaps with a non-coding transcript
     *
     * @param tx a non-coding transcript (this is checked by calling code)
     * @return
     */
    public Overlap genic(SvAnnTxModel tx, GenomicRegion event) {
        ExonPair exonPair = getAffectedExons(tx, event);
        boolean affectsCds = false; // note this can only only true if the SV is exonic and the transcript is coding
        if (tx.isCoding()) {
            GenomicRegion cds = StandardGenomicRegion.of(tx.getCdsStart(), tx.getCdsEnd());
            if (cds.overlapsWith(event) && exonPair.atLeastOneExonOverlap()) {
                affectsCds = true;
            }
        }
        String geneSymbol = tx.getGeneSymbol();
        String txAccession = tx.getAccession();
        if (exonPair.atLeastOneExonOverlap()) {
            // determine which exons are affected
            int firstAffectedExon = exonPair.getFirstAffectedExon();
            int lastAffectedExon = exonPair.getLastAffectedExon();
            if (firstAffectedExon == lastAffectedExon) {
                String msg = String.format("%s/%s[exon %d]",
                        geneSymbol,
                        txAccession,
                        firstAffectedExon);
                OverlapDistance odist = OverlapDistance.fromExonic(geneSymbol, affectsCds);
                return new Overlap(SINGLE_EXON_IN_TRANSCRIPT, tx, odist, msg);
            } else {
                String msg = String.format("%s/%s[exon %d-%d]",
                        geneSymbol,
                        txAccession,
                        firstAffectedExon,
                        lastAffectedExon);
                OverlapDistance odist = OverlapDistance.fromExonic(geneSymbol, affectsCds);
                return new Overlap(MULTIPLE_EXON_IN_TRANSCRIPT, tx, odist, msg);
            }
        } else {
            // if we get here, then both positions must be in the same intron
            IntronDistance intronDist = getIntronNumber(tx, event);
            String msg = String.format("%s/%s[intron %d]", geneSymbol, txAccession, intronDist.getIntronNumber());
            OverlapDistance odist = OverlapDistance.fromIntronic(geneSymbol, intronDist);
            return new Overlap(INTRONIC, tx, odist, msg);
        }
    }

    /**
     * This method is called if there is no overlap with any part of a transcript. If a structural variant
     * is located 5' to the nearest transcript, it is called upstream, and if it is 3' to the nearest
     * transcript, it is called downstream
     *
     * @param event   region representing the SV event, always on FWD strand
     * @param qresult Jannovar object with left and right neighbors of the SV
     * @return list of overlaps -- usually both the upstream and the downstream neighbors (unless the SV is at the very end/beginning of a chromosome)
     */
    private List<Overlap> intergenic(GenomicRegion event, IntervalArray<SvAnnTxModel>.QueryResult qresult) {
        List<Overlap> overlaps = new ArrayList<>(2);

        // This means that the SV does not overlap with any annotated transcript
        SvAnnTxModel txLeft = qresult.getLeft();
        SvAnnTxModel txRight = qresult.getRight();
        // if we are 5' or 3' to the first or last gene on the chromosome, then
        // there is not left or right gene anymore
        // TODO: 9. 11. 2020 test this situation!

        if (txLeft != null) {
            // process the transcript if not null
            Overlap overlap = getOverlapForTranscript(event, txLeft);
            overlaps.add(overlap);
        }
        if (txRight != null) {
            Overlap overlap = getOverlapForTranscript(event, txRight);
            overlaps.add(overlap);
        }
        return overlaps;
    }

    /**
     * This method checks the content of the inverted region.
     *
     * @param inversion inversion
     * @return list of transcript overlaps (can be empty)
     */
    private List<Overlap> getInversionOverlapsRegionBased(SequenceRearrangement inversion) {
        if (!inversion.getType().equals(SvType.INVERSION)) {
            return List.of();
        }
        SequenceRearrangement onFwd = inversion.withStrand(Strand.FWD);
        Breakend left = onFwd.getLeftmostBreakend();
        Breakend right = onFwd.getRightmostBreakend();
        // assume that breakends are on the same contig
//        GenomeInterval gi = new GenomeInterval(rd, de.charite.compbio.jannovar.reference.Strand.FWD, left.getContig().getId(), left.getPosition(), right.getPosition(), PositionType.ONE_BASED);

//        IntervalArray<TranscriptModel>.QueryResult qresult = chromosomeMap.get(left.getContig().getId()).getTMIntervalTree().findOverlappingWithInterval(left.getPosition(), right.getPosition());

//        return getOverlapList(inversion, qresult);
        // TODO: 9. 11. 2020 fix
        return List.of();
    }
}
