package org.jax.svanna.core.overlap;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.variant.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jax.svanna.core.overlap.OverlapType.*;


/**
 * This class determines the kind and degree of overlap of a structural variant with transcript features.
 * The class should be used via the {@link #getOverlapList(Variant)} function.
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

    /**
     * @param intervalArrayMap See {@link #intervalArrayMap}.
     */
    public SvAnnOverlapper(Map<Integer, IntervalArray<Transcript>> intervalArrayMap) {
        this.intervalArrayMap = intervalArrayMap;
    }

    /**
     * public interface to this class. We search for a list of overlaps that overlap the variant
     * @param variant A structural variant
     * @return list of {@link Overlap} objects that represent transcripts that overlap with the variant.
     */
    @Override
    public List<Overlap> getOverlapList(Variant variant) {
        switch (variant.variantType().baseType()) {
            case DEL:
            case DUP:
            case INS:
            case INV:
                GenomicRegion region = variant.toZeroBased().toPositiveStrand();
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
     * Calculate overlap for a coding or non-coding transcript. By assumption, if we get here then the transcript
     * is not entirely contained within the SV, instead, the SV overlaps with only a part of the transcript.
     * This function determines which parts of the transcript overlap. By assumption, the calling code has checked that
     * the SV does not entirely contain the transcript but instead overlaps with a part of it.
     * @param tx a coding or non-coding transcript
     * @param event -- Region corresponding to a SV.
     * @return An {@link Overlap} object for an SV that affects a gene
     */
    private static Overlap genic(Transcript tx, GenomicRegion event) {
        ExonPair exonPair = getAffectedExons(tx, event);
        boolean affectsCds = false; // note this can only only true if the SV is exonic and the transcript is coding
        if (tx.isCoding()) {
            GenomicRegion cds = tx.cdsRegion();
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
                // check if the exon is coding or not
                if (affectsCds) {
                    return new Overlap(SINGLE_EXON_IN_TRANSCRIPT, tx, overlapDistance, msg);
                } else if (tx.isCoding()) {
                    return new Overlap(NON_CDS_REGION_IN_SINGLE_EXON, tx, overlapDistance, msg);
                } else {
                    return new Overlap(SINGLE_EXON_IN_NC_TRANSCRIPT, tx, overlapDistance, msg);
                }
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

            String msg = String.format("%s/%s[%s]", geneSymbol, txAccession, intronDist.getUpDownStreamDistance(tx.strand().isPositive()));
            OverlapDistance odist = OverlapDistance.fromIntronic(geneSymbol, intronDist);
            return new Overlap(INTRONIC, tx, odist, msg);
        }
    }

    /**
     * If we get here, we know that the SV event is intrachromosomal (i.e., not a BND/translocation). This function
     * decides whether the SV overlaps with one or more transcripts and dispatches to
     * {@link #intergenic(GenomicRegion, IntervalArray.QueryResult)} if there is no overlap and to
     * {@link #parseEventThatOverlapsWithATranscript(GenomicRegion, List)} if there is.
     *
     * @param region      SV event
     * @param queryResult result with transcript features
     * @return overlap list
     */
    private static List<Overlap> parseIntrachromosomalEventQueryResult(GenomicRegion region,
                                                                       IntervalArray<Transcript>.QueryResult queryResult) {
        if (queryResult.getEntries().isEmpty()) {
            return intergenic(region, queryResult);
        }

        // if we get here, then we overlap with one or more genes
        return parseEventThatOverlapsWithATranscript(region, queryResult.getEntries());
    }

    /**
     * This function decides whether transcripts are entirely contained within the SV or whether the SV affects on
     * parts of a transcript.
     * @param region A genomic region corresponding to a structural variant
     * @param transcripts List of transcripts that overlap with the region
     * @return List of {@link Overlap} objects corresponding to the transcripts
     */
    private static List<Overlap> parseEventThatOverlapsWithATranscript(GenomicRegion region, List<Transcript> transcripts) {
        return transcripts.stream()
                .map(tx -> region.contains(tx)
                        ? new Overlap(TRANSCRIPT_CONTAINED_IN_SV, tx, OverlapDistance.fromContainedIn(), String.format("%s/%s", tx.hgvsSymbol(), tx.accessionId()))
                        : genic(tx, region))
                .collect(Collectors.toList());
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
            GenomicPosition onPos = bnd.toPositiveStrand();

            IntervalArray<Transcript> contigIArray = intervalArrayMap.get(onPos.contigId());
            List<Transcript> overlappingTxs = contigIArray.findOverlappingWithPoint(onPos.pos()).getEntries();
            GenomicRegion bndRegion = onPos.toRegion(0, 1);
            overlaps.addAll(parseEventThatOverlapsWithATranscript(bndRegion, overlappingTxs));
        }

        return overlaps;
    }
}
