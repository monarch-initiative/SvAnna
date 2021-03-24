package org.jax.svanna.core.overlap;

import com.google.common.collect.ImmutableList;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.reference.CodingTranscript;
import org.jax.svanna.core.reference.Exon;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.jax.svanna.core.overlap.OverlapType.*;

// TODO - this class should be removed/reworked in the final version, once we
//  settle upon the requirements
@SuppressWarnings("DuplicatedCode")
public class GeneOverlapper implements Overlapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeneOverlapper.class);

    private static final String GENE_PLACEHOLDER_SYMBOL = "*GENE*";

    private final Map<Integer, IntervalArray<Gene>> chromosomeMap;

    public GeneOverlapper(Map<Integer, IntervalArray<Gene>> chromosomeMap) {
        this.chromosomeMap = chromosomeMap;
    }

    @Override
    public List<Overlap> getOverlaps(Variant variant) {
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

    private List<Overlap> emptyRegionOverlap(GenomicRegion region) {
        return parseIntrachromosomalEventQueryResult(region, emptyRegionResults(region));
    }

    private List<Overlap> translocationOverlaps(BreakendVariant translocation) {
        List<Overlap> overlaps = new ArrayList<>();
        for (Breakend breakend : List.of(translocation.left(), translocation.right())) {
            // breakends are empty regions, as defined in Svart
            IntervalArray<Gene>.QueryResult breakendQuery = emptyRegionResults(breakend);
            ImmutableList<Gene> overlapping = breakendQuery.getEntries();
            if (overlapping.isEmpty()) {
                Transcript leftClosest = breakendQuery.getLeft() == null
                        ? null
                        : breakendQuery.getLeft().transcripts().stream()
                        .min(Comparator.comparingInt(tx -> tx.distanceTo(breakend)))
                        .orElse(null);
                Transcript rightClosest = breakendQuery.getRight() == null
                        ? null
                        : breakendQuery.getRight().transcripts().stream()
                        .min(Comparator.comparingInt(tx -> tx.distanceTo(breakend)))
                        .orElse(null);
                overlaps.addAll(intergenic(breakend, leftClosest, rightClosest));
            } else {
                overlapping.stream()
                        .map(Gene::transcripts)
                        .flatMap(Collection::stream)
                        .map(tx -> genic(tx, breakend))
                        .forEach(overlaps::add);
            }
        }

        return overlaps;
    }

    private IntervalArray<Gene>.QueryResult emptyRegionResults(GenomicRegion region) {
        IntervalArray<Gene> intervalArray = chromosomeMap.get(region.contigId());
        int start = region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()) - 1;
        int end = region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        return intervalArray.findOverlappingWithInterval(start, end);
    }

    private List<Overlap> intrachromosomalEventOverlaps(GenomicRegion region) {
        IntervalArray<Gene> intervalArray = chromosomeMap.get(region.contigId());
        int start = region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        int end = region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        return parseIntrachromosomalEventQueryResult(region, intervalArray.findOverlappingWithInterval(start, end));
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
    private static List<Overlap> parseIntrachromosomalEventQueryResult(GenomicRegion region,
                                                                       IntervalArray<Gene>.QueryResult queryResult) {
        if (queryResult.getEntries().isEmpty()) {
            Transcript leftClosest = queryResult.getLeft() == null
                    ? null
                    : queryResult.getLeft().transcripts().stream()
                    .min(Comparator.comparingInt(tx -> tx.distanceTo(region)))
                    .orElse(null);
            Transcript rightClosest = queryResult.getRight() == null
                    ? null
                    : queryResult.getRight().transcripts().stream()
                    .min(Comparator.comparingInt(tx -> tx.distanceTo(region)))
                    .orElse(null);
            return intergenic(region, leftClosest, rightClosest);
        }

        // if we get here, then we overlap with one or more genes
        List<Transcript> transcripts = queryResult.getEntries().stream()
                .map(Gene::transcripts)
                .flatMap(Collection::stream)
                .filter(tx -> tx.contigId() == region.contigId()) // TODO - resolve as this can happen can
                .collect(Collectors.toList());
        return parseEventThatOverlapsWithATranscript(region, transcripts);
    }

    /**
     * This method is called if there is no overlap with any part of a transcript. If a structural variant
     * is located 5' to the nearest transcript, it is called upstream, and if it is 3' to the nearest
     * transcript, it is called downstream
     *
     * @param event region representing the SV event, always on FWD strand
     * @param left  left transcript or null if we are at the chromosome edge
     * @param right right transcript or null if we are at the chromosome edge
     * @return list of overlaps -- usually both the upstream and the downstream neighbors (unless the SV is at the very end/beginning of a chromosome)
     */
    private static List<Overlap> intergenic(GenomicRegion event, Transcript left, Transcript right) {
        List<Overlap> overlaps = new ArrayList<>(2);

        if (left != null)
            overlaps.add(getOverlapForTranscript(event, left));
        if (right != null)
            overlaps.add(getOverlapForTranscript(event, right));
        return overlaps;
    }

    /**
     * This function decides whether transcripts are entirely contained within the SV or whether the SV affects on
     * parts of a transcript.
     *
     * @param region      A genomic region corresponding to a structural variant
     * @param transcripts List of transcripts that overlap with the region
     * @return List of {@link Overlap} objects corresponding to the transcripts
     */
    private static List<Overlap> parseEventThatOverlapsWithATranscript(GenomicRegion region, List<Transcript> transcripts) {
        List<Overlap> overlaps = new ArrayList<>(transcripts.size());
        for (Transcript tx : transcripts) {
            Overlap overlap;
            if (region.contains(tx)) {
                overlap = new Overlap(TRANSCRIPT_CONTAINED_IN_SV, tx, GENE_PLACEHOLDER_SYMBOL, OverlapDistance.fromContainedIn(), String.format("%s/%s", GENE_PLACEHOLDER_SYMBOL, tx.accessionId()));
            } else if (region.overlapsWith(tx)) {
                overlap = genic(tx, region);
            } else {
                overlap = getOverlapForTranscript(region, tx);
            }
            overlaps.add(overlap);
        }
        return overlaps;
    }

    private static Overlap getOverlapForTranscript(GenomicRegion event, Transcript tx) {
        // get the closest distance
        int distance = tx.distanceTo(event);

        // If we get here, we know that the tx does not overlap with the event.
        // If the distance is positive, then the event is upstream from the tx.
        // Otherwise, the event is downstream wrt. the tx.
        OverlapType type;
        OverlapDistance overlapDistance;
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
            overlapDistance = OverlapDistance.fromUpstreamFlankingGene(distance, tx.accessionId());
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
            overlapDistance = OverlapDistance.fromDownstreamFlankingGene(distance, tx.accessionId());
        }

        return new Overlap(type, tx, GENE_PLACEHOLDER_SYMBOL, overlapDistance);
    }

    /**
     * Calculate overlap for a coding or non-coding transcript. By assumption, if we get here then the transcript
     * is not entirely contained within the SV, instead, the SV overlaps with only a part of the transcript.
     * This function determines which parts of the transcript overlap. By assumption, the calling code has checked that
     * the SV does not entirely contain the transcript but instead overlaps with a part of it.
     *
     * @param tx    a coding or non-coding transcript
     * @param event -- Region corresponding to a SV.
     * @return An {@link Overlap} object for an SV that affects a gene
     */
    private static Overlap genic(Transcript tx, GenomicRegion event) {
        ExonPair exonPair = getAffectedExons(tx, event);
        boolean affectsCds = false; // note this can only only true if the SV is exonic and the transcript is coding
        if (tx instanceof CodingTranscript) {
            CodingTranscript ctx = (CodingTranscript) tx;
            boolean overlap = Coordinates.overlap(ctx.coordinateSystem(), ctx.codingStart(), ctx.codingEnd(), event.coordinateSystem(), event.start(), event.end());
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
                    return new Overlap(SINGLE_EXON_IN_TRANSCRIPT, tx, GENE_PLACEHOLDER_SYMBOL, overlapDistance, msg);
                } else if (tx.isCoding()) {
                    return new Overlap(NON_CDS_REGION_IN_SINGLE_EXON, tx, GENE_PLACEHOLDER_SYMBOL, overlapDistance, msg);
                } else {
                    return new Overlap(SINGLE_EXON_IN_NC_TRANSCRIPT, tx, GENE_PLACEHOLDER_SYMBOL, overlapDistance, msg);
                }
            } else {
                String msg = String.format("%s/%s[exon %d-%d]",
                        geneSymbol,
                        txAccession,
                        firstAffectedExon,
                        lastAffectedExon);
                OverlapDistance odist = OverlapDistance.fromExonic(geneSymbol, affectsCds);
                return new Overlap(MULTIPLE_EXON_IN_TRANSCRIPT, tx, GENE_PLACEHOLDER_SYMBOL, odist, msg);
            }
        } else {
            // if we get here, then both positions must be in the same intron
            IntronDistance intronDist = getIntronNumber(tx, event);

            String msg = String.format("%s/%s[%s]", geneSymbol, txAccession, intronDist.getUpDownStreamDistance(tx.strand().isPositive()));
            OverlapDistance odist = OverlapDistance.fromIntronic(geneSymbol, intronDist);
            return new Overlap(INTRONIC, tx, GENE_PLACEHOLDER_SYMBOL, odist, msg);
        }
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
        event = event.withStrand(tx.strand());
        List<Exon> exons = tx.exons();
        boolean[] affected = new boolean[exons.size()]; // initializes to false
        for (int i = 0; i < exons.size(); i++) {
            Exon exon = exons.get(i);
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
    private static IntronDistance getIntronNumber(Transcript tx, GenomicRegion variant) {
        // we use zero based coordinates for calculations
        int variantStart = variant.startOnStrandWithCoordinateSystem(tx.strand(), CoordinateSystem.zeroBased());
        int variantEnd = variant.endOnStrandWithCoordinateSystem(tx.strand(), CoordinateSystem.zeroBased());
        List<Exon> exons = tx.exons();

        for (int i = 0; i < exons.size() - 1; i++) {
            // current exon end
            int intronStart = exons.get(i).endWithCoordinateSystem(CoordinateSystem.zeroBased());
            // next exon start
            int intronEnd = exons.get(i + 1).startWithCoordinateSystem(CoordinateSystem.zeroBased());

            if (intronStart <= variantStart && variantEnd <= intronEnd) {
                // we start the intron numbering at 1
                int intronNumber = i + 1;
                int up = variantStart - intronStart;
                int down = intronEnd - variantEnd;
                return new IntronDistance(intronNumber, up, down);
            }

        }
        LogUtils.logWarn(LOGGER, "Could not find intron number");
        return IntronDistance.empty();
    }
}
