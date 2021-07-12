package org.jax.svanna.core.overlap;

import com.google.common.collect.ImmutableList;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.reference.CodingTranscript;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jax.svanna.core.overlap.OverlapType.*;


/**
 * This class determines the kind and degree of overlap of a structural variant with transcript features.
 * The class should be used via the {@link #getOverlaps(Variant)} function.
 */
public class SvAnnOverlapper implements Overlapper {

    // TODO - remove
    private static final String GENE_PLACEHOLDER_SYMBOL = "*GENE*";

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
     * @return list of {@link TranscriptOverlap} objects that represent transcripts that overlap with the variant.
     */
    @Override
    public List<TranscriptOverlap> getOverlaps(Variant variant) {
        switch (variant.variantType().baseType()) {
            case DEL:
            case DUP:
            case INV:
            case CNV:
                return intrachromosomalEventOverlaps(variant);
            case INS:
                return variant.length() == 0 ? emptyRegionOverlap(variant) : intrachromosomalEventOverlaps(variant);
            case TRA:
            case BND:
                if (variant instanceof BreakendVariant) {
                    return translocationOverlaps(((BreakendVariant) variant));
                } else {
                    LogUtils.logWarn(LOGGER, "Variant `{}` has type `{}` but it is not instance of BreakendVariant", LogUtils.variantSummary(variant), variant.variantType());
                    return List.of();
                }
            default:
                LogUtils.logWarn(LOGGER, "Getting overlaps for `{}` is not yet supported", variant.variantType());
                return List.of();
        }
    }

    private List<TranscriptOverlap> intrachromosomalEventOverlaps(GenomicRegion region) {
        IntervalArray<Transcript> intervalArray = intervalArrayMap.get(region.contigId());
        int start = region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        int end = region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        return parseIntrachromosomalEventQueryResult(region, intervalArray.findOverlappingWithInterval(start, end));
    }

    private List<TranscriptOverlap> emptyRegionOverlap(GenomicRegion region) {
        return parseIntrachromosomalEventQueryResult(region, emptyRegionResults(region));
    }

    private List<TranscriptOverlap> translocationOverlaps(BreakendVariant translocation) {
        List<TranscriptOverlap> overlaps = new ArrayList<>();
        for (Breakend breakend : List.of(translocation.left(), translocation.right())) {
            // breakends are empty regions, as defined in Svart
            IntervalArray<Transcript>.QueryResult breakendQuery = emptyRegionResults(breakend);
            ImmutableList<Transcript> overlapping = breakendQuery.getEntries();
            if (overlapping.isEmpty()) {
                overlaps.addAll(intergenic(breakend, breakendQuery.getLeft(), breakendQuery.getRight()));
            } else {
                overlapping.stream()
                        .map(tx -> genic(breakend, tx))
                        .forEach(overlaps::add);
            }
        }

        return overlaps;
    }

    private IntervalArray<Transcript>.QueryResult emptyRegionResults(GenomicRegion region) {
        IntervalArray<Transcript> intervalArray = intervalArrayMap.get(region.contigId());
        int start = region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()) - 1;
        int end = region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        return intervalArray.findOverlappingWithInterval(start, end);
    }

    /**
     * If we get here, we know that the SV event is intrachromosomal (i.e., not a BND/translocation). This function
     * decides whether the SV overlaps with one or more transcripts and dispatches to
     * {@link #intergenic} if there is no overlap and to
     * {@link #parseEventThatOverlapsWithATranscript(GenomicRegion, List)} if there is.
     *
     * @param region      SV event
     * @param queryResult result with transcript features
     * @return overlap list
     */
    private static List<TranscriptOverlap> parseIntrachromosomalEventQueryResult(GenomicRegion region,
                                                                                 IntervalArray<Transcript>.QueryResult queryResult) {
        if (queryResult.getEntries().isEmpty()) {
            return intergenic(region, queryResult.getLeft(), queryResult.getRight());
        }

        // if we get here, then we overlap with one or more genes
        return parseEventThatOverlapsWithATranscript(region, queryResult.getEntries());
    }

    /**
     * This method is called if there is no overlap with any part of a transcript. If a structural variant
     * is located 5' to the nearest transcript, it is called upstream, and if it is 3' to the nearest
     * transcript, it is called downstream
     *
     * @param region       region representing the SV event, always on FWD strand
     * @param left left transcript or null if we are at the chromosome edge
     * @param right right transcript or null if we are at the chromosome edge
     * @return list of overlaps -- usually both the upstream and the downstream neighbors (unless the SV is at the very end/beginning of a chromosome)
     */
    private static List<TranscriptOverlap> intergenic(GenomicRegion region, Transcript left, Transcript right) {
        List<TranscriptOverlap> overlaps = new ArrayList<>(2);

        if (left != null) {
            overlaps.add(getOverlapForTranscript(region, left));
        }
        if (right != null) {
            overlaps.add(getOverlapForTranscript(region, right));
        }
        return overlaps;
    }

    private static TranscriptOverlap getOverlapForTranscript(GenomicRegion region, Transcript tx) {
        // get the closest distance
        int distance = tx.distanceTo(region);
        // If we get here, we know that the tx does not overlap with the event.
        // If the distance is positive, then the event is upstream from the tx.
        // Otherwise, the event is downstream wrt. the tx.
        OverlapType type;
        OverlapDistance overlapDistance;
//        String hgvsSymbol = tx.hgvsSymbol();
        String hgvsSymbol = GENE_PLACEHOLDER_SYMBOL;
        if (distance < 0) {
            // event is upstream from the transcript
            if (distance >= -500) {
                type = UPSTREAM_GENE_VARIANT_500B;
            } else if (distance >= -2_000) {
                type = UPSTREAM_GENE_VARIANT_2KB;
            } else if (distance >= -5_000) {
                type = UPSTREAM_GENE_VARIANT_5KB;
            } else if (distance >= -500_000) {
                type = UPSTREAM_GENE_VARIANT_500KB;
            } else {
                type = UPSTREAM_GENE_VARIANT;
            }
            overlapDistance = OverlapDistance.fromUpstreamFlankingGene(distance, hgvsSymbol);
        } else {
            // event is downstream from the tx
            if (distance <= 500) {
                type = DOWNSTREAM_GENE_VARIANT_500B;
            } else if (distance <= 2_000) {
                type = DOWNSTREAM_GENE_VARIANT_2KB;
            } else if (distance <= 5_000) {
                type = DOWNSTREAM_GENE_VARIANT_5KB;
            } else if (distance <= 500_000) {
                type = DOWNSTREAM_GENE_VARIANT_500KB;
            } else {
                type = DOWNSTREAM_GENE_VARIANT;
            }
            overlapDistance = OverlapDistance.fromDownstreamFlankingGene(distance, hgvsSymbol);
        }

        return TranscriptOverlap.of(type,  tx, hgvsSymbol, overlapDistance, overlapDistance.getDescription());
    }

    /**
     * This function decides whether transcripts are entirely contained within the SV or whether the SV affects on
     * parts of a transcript.
     * @param region A genomic region corresponding to a structural variant
     * @param transcripts List of transcripts that overlap with the region
     * @return List of {@link TranscriptOverlap} objects corresponding to the transcripts
     */
    private static List<TranscriptOverlap> parseEventThatOverlapsWithATranscript(GenomicRegion region, List<Transcript> transcripts) {
        return transcripts.stream()
                .map(tx -> region.contains(tx)
                        ? TranscriptOverlap.of(TRANSCRIPT_CONTAINED_IN_SV, tx, GENE_PLACEHOLDER_SYMBOL, OverlapDistance.fromContainedIn(), String.format("%s/%s", GENE_PLACEHOLDER_SYMBOL, tx.accessionId()))
                        : genic(region, tx))
                .collect(Collectors.toList());
    }

    /**
     * Calculate overlap for a coding or non-coding transcript. By assumption, if we get here then the transcript
     * is not entirely contained within the SV, instead, the SV overlaps with only a part of the transcript.
     * This function determines which parts of the transcript overlap. By assumption, the calling code has checked that
     * the SV does not entirely contain the transcript but instead overlaps with a part of it.
     * @param region -- Region corresponding to a SV.
     * @param tx a coding or non-coding transcript
     * @return An {@link TranscriptOverlap} object for an SV that affects a gene
     */
    private static TranscriptOverlap genic(GenomicRegion region, Transcript tx) {
        ExonPair exonPair = Utils.getAffectedExons(region, tx);
        boolean affectsCds = false; // note this can only only true if the SV is exonic and the transcript is coding
        if (tx instanceof CodingTranscript) {
            CodingTranscript ctx = (CodingTranscript) tx;
            boolean overlap = Coordinates.overlap(ctx.coordinateSystem(), ctx.codingStart(), ctx.codingEnd(), region.coordinateSystem(), region.start(), region.end());
            affectsCds = overlap && exonPair.atLeastOneExonOverlap();
        }
        String geneSymbol = GENE_PLACEHOLDER_SYMBOL;
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
                    return TranscriptOverlap.of(SINGLE_EXON_IN_TRANSCRIPT, tx, GENE_PLACEHOLDER_SYMBOL, overlapDistance, msg);
                } else if (tx.isCoding()) {
                    return TranscriptOverlap.of(NON_CDS_REGION_IN_SINGLE_EXON, tx, GENE_PLACEHOLDER_SYMBOL, overlapDistance, msg);
                } else {
                    return TranscriptOverlap.of(SINGLE_EXON_IN_NC_TRANSCRIPT, tx, GENE_PLACEHOLDER_SYMBOL, overlapDistance, msg);
                }
            } else {
                String msg = String.format("%s/%s[exon %d-%d]",
                        geneSymbol,
                        txAccession,
                        firstAffectedExon,
                        lastAffectedExon);
                OverlapDistance odist = OverlapDistance.fromExonic(geneSymbol, affectsCds);
                return TranscriptOverlap.of(MULTIPLE_EXON_IN_TRANSCRIPT, tx, GENE_PLACEHOLDER_SYMBOL, odist, msg);
            }
        } else {
            // if we get here, then both positions must be in the same intron
            IntronDistance intronDist = Utils.getIntronNumber(region, tx);

            String msg = String.format("%s/%s[%s]", geneSymbol, txAccession, intronDist.getUpDownStreamDistance(tx.strand().isPositive()));
            OverlapDistance odist = OverlapDistance.fromIntronic(geneSymbol, intronDist);
            return TranscriptOverlap.of(INTRONIC, tx, GENE_PLACEHOLDER_SYMBOL, odist, msg);
        }
    }

}
