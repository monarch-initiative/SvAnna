package org.jax.svanna.core.overlap;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.variant.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jax.svanna.core.overlap.OverlapType.*;


/**
 * This class determines the kind and degree of overlap of a structural variant with transcript features.
 */
public class SvAnnOverlapper implements Overlapper {


    /*
     * Implementation notes
     * - variant must be flipped to FWD strand before querying the interval tree
     */

    private static final Logger LOGGER = LoggerFactory.getLogger(SvAnnOverlapper.class);

    /**
     * Map where key corresponds to {@link Contig#id()} and the value contains interval array with the
     * transcripts.
     */
    private final Map<Integer, IntervalArray<Transcript>> intervalArrayMap;

    public SvAnnOverlapper(Map<Integer, IntervalArray<Transcript>> intervalArrayMap) {
        this.intervalArrayMap = intervalArrayMap;
    }

    private static Overlap getOverlapForTranscript(GenomicRegion event, Transcript tx) {
        GenomicRegion eventOnStrand = event.withStrand(tx.strand());
        // get the closest distance
        int startDistance = eventOnStrand.startGenomicPosition().distanceTo(tx.startGenomicPosition());
        int endDistance = eventOnStrand.startGenomicPosition().distanceTo(tx.endGenomicPosition());
        int distance = Math.abs(startDistance) < Math.abs(endDistance)
                ? startDistance
                : endDistance;
        // If we get here, we know that the tx does not overlap with the event.
        // If the distance is positive, then the event is upstream from the tx.
        // Otherwise, the event is downstream wrt. the tx.
        OverlapType type;
        OverlapDistance overlapDistance;
        if (distance > 0) {
            // event is upstream from the transcript
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
            overlapDistance = OverlapDistance.fromUpstreamFlankingGene(distance, tx.hgvsSymbol());
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
            overlapDistance = OverlapDistance.fromDownstreamFlankingGene(distance, tx.hgvsSymbol());
        }

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
    private static ExonPair getAffectedExons(Transcript tx, GenomicRegion event) {
        List<GenomicRegion> exons = tx.exons();
        boolean[] affected = new boolean[exons.size()]; // initializes to false
        for (int i = 0; i < exons.size(); i++) {
            GenomicRegion exon = exons.get(i);
            if (exon.overlapsWith(event)) {
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
    private static IntronDistance getIntronNumber(Transcript tx, GenomicRegion event) {
        List<GenomicRegion> exons = tx.exons();

        for (int i = 0; i < exons.size() - 1; i++) {
            // current exon end
            GenomicPosition intronStart = exons.get(i).endGenomicPosition();
            // next exon start
            GenomicPosition intronEnd = exons.get(i + 1).startGenomicPosition();

            if (intronStart.isUpstreamOf(event.startGenomicPosition()) && intronEnd.isDownstreamOf(event.endGenomicPosition())) {
                // we start the intron numbering at 1
                int intronNumber = i + 1;
                int up = event.startGenomicPosition().distanceTo(intronStart);
                int down = event.endGenomicPosition().distanceTo(intronEnd);
                return new IntronDistance(intronNumber, up, down);
            }

        }
        LOGGER.warn("Could not find intron number");
        return IntronDistance.empty();
    }

    /**
     * This method is called if there is no overlap with any part of a transcript. If a structural variant
     * is located 5' to the nearest transcript, it is called upstream, and if it is 3' to the nearest
     * transcript, it is called downstream
     *
     * @param event       region representing the SV event, always on FWD strand
     * @param queryResult Jannovar object with left and right neighbors of the SV
     * @return list of overlaps -- usually both the upstream and the downstream neighbors (unless the SV is at the very end/beginning of a chromosome)
     */
    private static List<Overlap> intergenic(GenomicRegion event, IntervalArray<Transcript>.QueryResult queryResult) {
        List<Overlap> overlaps = new ArrayList<>(2);

        // This means that the SV does not overlap with any annotated transcript
        Transcript txLeft = queryResult.getLeft();
        Transcript txRight = queryResult.getRight();
        // if we are 5' or 3' to the first or last gene on the chromosome, then
        // there is not left or right gene anymore

        if (txLeft != null) {
            // process the transcript if not null
            overlaps.add(getOverlapForTranscript(event, txLeft));
        }
        if (txRight != null) {
            overlaps.add(getOverlapForTranscript(event, txRight));
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
    public static Overlap genic(Transcript tx, GenomicRegion event) {
        ExonPair exonPair = getAffectedExons(tx, event);
        boolean affectsCds = false; // note this can only only true if the SV is exonic and the transcript is coding
        if (tx.isCoding()) {
            GenomicRegion cds = GenomicRegion.of(tx.cdsStart(), tx.cdsEnd());
            if (cds.overlapsWith(event) && exonPair.atLeastOneExonOverlap()) {
                affectsCds = true;
            }
        }
        String geneSymbol = tx.hgvsSymbol();
        String txAccession = tx.accessionId();
        if (exonPair.atLeastOneExonOverlap()) {
            // determine which exons are affected
            int firstAffectedExon = exonPair.getFirstAffectedExon();
            int lastAffectedExon = exonPair.getLastAffectedExon();
            if (firstAffectedExon == lastAffectedExon) {
                String msg = String.format("%s/%s[exon %d]",
                        geneSymbol,
                        txAccession,
                        firstAffectedExon);
                OverlapDistance overlapDistance = OverlapDistance.fromExonic(geneSymbol, affectsCds);
                return new Overlap(SINGLE_EXON_IN_TRANSCRIPT, tx, overlapDistance, msg);
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
     * If we get here, we know that the SV event is intrachromosomal.
     *
     * @param region      SV event
     * @param queryResult result with transcript features
     * @return overlap list
     */
    private static List<Overlap> parseIntrachromosomalEventQueryResult(GenomicRegion region,
                                                                       IntervalArray<Transcript>.QueryResult queryResult) {
        List<Overlap> overlaps = new ArrayList<>();
        if (queryResult.getEntries().isEmpty()) {
            return intergenic(region, queryResult);
        }
        // if we get here, then we overlap with one or more genes
        List<Transcript> overlappingTranscripts = queryResult.getEntries();
        for (var tx : overlappingTranscripts) {
            if (region.contains(tx)) {
                // the transcript is completely contained in the SV
                String msg = String.format("%s/%s", tx.hgvsSymbol(), tx.accessionId());
                Overlap overlap = new Overlap(TRANSCRIPT_CONTAINED_IN_SV, tx, OverlapDistance.fromContainedIn(), msg);
                overlaps.add(overlap);
                continue;
            }
            if (!tx.contains(region.startGenomicPosition()) && !tx.contains(region.endGenomicPosition())) {
                LOGGER.error("Warning, transcript model ({};{}) retrieved that does not overlap (chr{}:{}-{}({})): ",
                        tx.hgvsSymbol(), tx.accessionId(), region.contigName(), region.start(), region.end(), region.strand());
                // TODO I observed this once, it should never happen and may be a Jannovar bug or have some other cause
            }
            // TODO if the above bug no longer occurs, make a regular if/else with the above
            Overlap overlap = genic(tx, region);
            overlaps.add(overlap);
        }
        if (overlaps.isEmpty()) {
            LOGGER.error("Could not find any overlaps with this query result: {}", queryResult);
            throw new SvAnnRuntimeException("Empty overlap list");
        }
        return overlaps;
    }

    @Override
    public List<Overlap> getOverlapList(Variant variant) {
        switch (variant.variantType().baseType()) {
            case DEL:
            case DUP:
            case INS:
            case INV:
                GenomicRegion region = variant.withStrand(Strand.POSITIVE).toZeroBased();
                return getIntrachromosomalEventOverlaps(region);
            case TRA:
            case BND:
                if (variant instanceof Breakended)
                    return getTranslocationOverlaps(((Breakended) variant));
            default:
                LOGGER.warn("Getting overlaps for `{}` is not yet supported", variant.variantType());
                return List.of();
        }
    }

    private List<Overlap> getIntrachromosomalEventOverlaps(GenomicRegion region) {
        IntervalArray<Transcript> intervalArray = intervalArrayMap.get(region.contigId());
        IntervalArray<Transcript>.QueryResult result = intervalArray.findOverlappingWithInterval(region.start(), region.end());
        return parseIntrachromosomalEventQueryResult(region, result);
    }

    private List<Overlap> getTranslocationOverlaps(Breakended translocation) {
        List<Overlap> overlaps = new ArrayList<>();
        // process overlaps
        for (Breakend bnd : List.of(translocation.left(), translocation.right())) {
            Breakend onFwd = bnd.withStrand(Strand.POSITIVE);

            IntervalArray<Transcript> contigIArray = intervalArrayMap.get(onFwd.contigId());
            IntervalArray<Transcript>.QueryResult leftQueryResults = contigIArray.findOverlappingWithPoint(onFwd.pos());
            GenomicRegion leftRegion = onFwd.toRegion(0, 1);

            overlaps.addAll(parseIntrachromosomalEventQueryResult(leftRegion, leftQueryResults));
        }

        return overlaps;
    }
}
