package org.jax.svanna.core.overlap;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.reference.CodingTranscript;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.jax.svanna.core.overlap.OverlapType.*;

public class IntervalArrayGeneOverlapper implements GeneOverlapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntervalArrayGeneOverlapper.class);

    /**
     * Map where key corresponds to {@link Contig#id()} and the value contains interval array with the
     * genes.
     */
    private final Map<Integer, IntervalArray<Gene>> geneIntervalArrays;

    public IntervalArrayGeneOverlapper(Map<Integer, IntervalArray<Gene>> geneIntervalArrays) {
        this.geneIntervalArrays = geneIntervalArrays;
    }

    @Override
    public List<GeneOverlap> getOverlaps(Variant variant) {
        switch (variant.variantType().baseType()) {
            case DEL:
            case DUP:
            case INV:
            case CNV:
            case SNV:
            case MNV:
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

    private List<GeneOverlap> intrachromosomalEventOverlaps(GenomicRegion region) {
        if (!geneIntervalArrays.containsKey(region.contigId())) {
            LogUtils.logDebug(LOGGER, "Unknown contig {}({})", region.contigId(), region.contigName());
            return List.of();
        }

        IntervalArray<Gene> array = geneIntervalArrays.get(region.contigId());
        int start = region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        int end = region.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        return parseIntrachromosomalEventQueryResult(region, array.findOverlappingWithInterval(start, end));

    }

    private List<GeneOverlap> emptyRegionOverlap(GenomicRegion region) {
        if (!geneIntervalArrays.containsKey(region.contigId())) {
            LogUtils.logDebug(LOGGER, "Unknown contig {}({})", region.contigId(), region.contigName());
            return List.of();
        }

        return parseIntrachromosomalEventQueryResult(region, emptyRegionResults(region));
    }

    private List<GeneOverlap> translocationOverlaps(BreakendVariant breakendVariant) {
        List<GeneOverlap> overlaps = new ArrayList<>();

        for (Breakend breakend : new Breakend[]{breakendVariant.left(), breakendVariant.right()}) {
            // breakends are empty regions, as defined in Svart
            IntervalArray<Gene>.QueryResult result = emptyRegionResults(breakend);
            overlaps.addAll(parseIntrachromosomalEventQueryResult(breakend, result));
        }

        return overlaps;
    }

    private IntervalArray<Gene>.QueryResult emptyRegionResults(GenomicRegion region) {
        IntervalArray<Gene> intervalArray = geneIntervalArrays.get(region.contigId());
        int start = region.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        return start == 0
                ? intervalArray.findOverlappingWithPoint(start + 1)
                : intervalArray.findOverlappingWithPoint(start);
    }


    private static List<GeneOverlap> parseIntrachromosomalEventQueryResult(GenomicRegion region, IntervalArray<Gene>.QueryResult result) {
        return result.getEntries().isEmpty()
                ? intergenic(region, result.getLeft(), result.getRight())
                : parseEventThatOverlapsWithAGene(region, result.getEntries());
    }

    private static List<GeneOverlap> intergenic(GenomicRegion region, Gene left, Gene right) {
        List<GeneOverlap> overlaps = new ArrayList<>(2);

        if (left != null)
            overlaps.add(processOverlapForGene(region, left));
        if (right != null)
            overlaps.add(processOverlapForGene(region, right));

        return overlaps;
    }

    private static List<GeneOverlap> parseEventThatOverlapsWithAGene(GenomicRegion region, Collection<Gene> genes) {
        return genes.stream()
                .map(gene -> processOverlapForGene(region, gene))
                .collect(Collectors.toList());
    }

    private static GeneOverlap processOverlapForGene(GenomicRegion region, Gene gene) {
        Map<String, TranscriptOverlap> overlaps = new HashMap<>(gene.transcripts().size());

        for (Transcript transcript : gene.transcripts()) {
            overlaps.put(transcript.accessionId(), processOverlapForTranscript(region, transcript));
        }

        return GeneOverlap.of(gene, overlaps);
    }

    private static TranscriptOverlap processOverlapForTranscript(GenomicRegion region, Transcript transcript) {
        return region.overlapsWith(transcript)
                ? processOverlappingTranscript(region, transcript)
                : processNonOverlappingTranscript(region, transcript);

    }

    private static TranscriptOverlap processOverlappingTranscript(GenomicRegion region, Transcript transcript) {
        String accessionId = transcript.accessionId();
        if (region.contains(transcript))
            return TranscriptOverlap.of(TRANSCRIPT_CONTAINED_IN_SV, accessionId, OverlapDistance.fromContainedIn(), accessionId);

        ExonPair affectedExons = Utils.getAffectedExons(region, transcript);
        boolean affectsCds = false; // note this can only only true if the SV is exonic and the transcript is coding
        if (transcript instanceof CodingTranscript) {
            CodingTranscript ctx = (CodingTranscript) transcript;
            boolean overlap = Coordinates.overlap(ctx.coordinateSystem(), ctx.codingStart(), ctx.codingEnd(),
                    region.coordinateSystem(), region.startOnStrand(ctx.strand()), region.endOnStrand(ctx.strand()));
            affectsCds = overlap && affectedExons.atLeastOneExonOverlap();
        }

        // TSS is defined as a 1bp-long region that spans the first base of the transcript
        if (Coordinates.aContainsB(region.coordinateSystem(), region.startOnStrand(transcript.strand()), region.endOnStrand(transcript.strand()),
                CoordinateSystem.oneBased(), transcript.startWithCoordinateSystem(CoordinateSystem.oneBased()), transcript.startWithCoordinateSystem(CoordinateSystem.oneBased()))) {
            OverlapType overlapType = transcript instanceof CodingTranscript ? AFFECTS_CODING_TRANSCRIPT_TSS : AFFECTS_NONCODING_TRANSCRIPT_TSS;
            return TranscriptOverlap.of(overlapType, accessionId, OverlapDistance.fromExonic(accessionId, affectsCds), accessionId);
        }

        if (affectedExons.atLeastOneExonOverlap()) {
            // determine which exons are affected
            int firstAffectedExon = affectedExons.getFirstAffectedExon();
            int lastAffectedExon = affectedExons.getLastAffectedExon();
            if (firstAffectedExon == lastAffectedExon) {
                String msg = String.format("%s[exon %d]", accessionId, firstAffectedExon);
                OverlapDistance overlapDistance = OverlapDistance.fromExonic(accessionId, affectsCds);
                // check if the exon is coding or not
                if (affectsCds)
                    return TranscriptOverlap.of(SINGLE_EXON_IN_TRANSCRIPT, accessionId, overlapDistance, msg);
                else if (transcript.isCoding())
                    return TranscriptOverlap.of(NON_CDS_REGION_IN_SINGLE_EXON, accessionId, overlapDistance, msg);
                else
                    return TranscriptOverlap.of(SINGLE_EXON_IN_NC_TRANSCRIPT, accessionId, overlapDistance, msg);
            } else {
                String msg = String.format("%s[exon %d-%d]", accessionId, firstAffectedExon, lastAffectedExon);
                OverlapDistance od = OverlapDistance.fromExonic(accessionId, affectsCds);
                return TranscriptOverlap.of(MULTIPLE_EXON_IN_TRANSCRIPT, accessionId, od, msg);
            }
        } else {
            // if we get here, then both positions must be in the same intron
            IntronDistance intronDist = Utils.getIntronNumber(region, transcript);

            String msg = String.format("%s[%s]", accessionId, intronDist.getUpDownStreamDistance(transcript.strand().isPositive()));
            OverlapDistance od = OverlapDistance.fromIntronic(accessionId, intronDist);
            return TranscriptOverlap.of(INTRONIC, accessionId, od, msg);
        }
    }

    private static TranscriptOverlap processNonOverlappingTranscript(GenomicRegion region, Transcript transcript) {
        int distance = transcript.distanceTo(region);

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
            overlapDistance = OverlapDistance.fromUpstreamFlankingGene(distance, transcript.accessionId());
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
            overlapDistance = OverlapDistance.fromDownstreamFlankingGene(distance, transcript.accessionId());
        }

        return TranscriptOverlap.of(type, transcript.accessionId(), overlapDistance, overlapDistance.getDescription());
    }
}
