package org.jax.svann.priority;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.overlap.OverlapType;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class prioritizes structural variants according to the sequences they affect. It does not take
 * into account any notion of phenotypic relevance. The rules are
 * <ol>
 *     <li>HIGH: High -- multiple genes, exonic, Disruption of a promoter (2kb upstream). Disruption of an enhancer.
 *     For SVs that affect exons, we only count HIGH priority if the SV affects the coding sequence (CDS).</li>
 *     <li>INTERMEDIATE. Intronic, downstream (up to 2kb). For coding genes, exonic SVs that do not afffect the CDS</li>
 *     <li>LOW: intergenic but not enhancer. Upstream/downstream, more than 2kb.</li>
 * </ol>
 * If the user has provided HPO terms describing the phenotypes observed in a proband, then we intend objects
 * of this class to subsequently be passed to {@link PhenotypeSvPrioritizer}.
 * @author Daniel Danis
 * @author Peter N Robinson
 */
@Deprecated // in favor of PrototypeSvPrioritizer
public class SequenceSvPrioritizer implements SvPrioritizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SequenceSvPrioritizer.class);
    /** In the first pass, we just look at sequence and do not analyze diseases, so this is the empty set.*/
    private static final Set<HpoDiseaseSummary> EMPTYSET = Set.of();
    /**
     * Genome assembly in use.
     */
    private final GenomeAssembly assembly;
    /**
     * The overlapper tests overlap of structural variants with transcripts.
     */
    private final Overlapper overlapper;
    /**
     * The enhancer overlapper tests overlap of structural variants with enhancers.
     */
    private final EnhancerOverlapper enhancerOverlapper;
    /**
     * Key -- gene symbol, value, {@link GeneWithId} object with symbol and id
     */
    private final Map<String, GeneWithId> geneSymbolMap;

    /**
     * @param assembly An object representing the assembly, e.g., HG38
     * @param enhancerMap A map of enhancers
     * @param geneSymbolMap A map of gene symbols and NCBI ids
     * @param jannovarData An object that contains representations of all transcripts
     */
    public SequenceSvPrioritizer(GenomeAssembly assembly,
                                 Map<Integer, IntervalArray<Enhancer>> enhancerMap,
                                 Map<String, GeneWithId> geneSymbolMap,
                                 JannovarData jannovarData) {
        this.assembly = assembly;
        this.geneSymbolMap = geneSymbolMap;
        this.overlapper = new Overlapper(jannovarData);
        this.enhancerOverlapper = new EnhancerOverlapper(jannovarData, enhancerMap);
    }


    //    @Override
    public SvPriority prioritize(SvPriority prioritizedRearrangement) {
        SequenceRearrangement rearrangement = prioritizedRearrangement.getRearrangement();
        switch (rearrangement.getType()) {
            case DELETION:
                return prioritizeDeletion(rearrangement);
            case INVERSION:
                return prioritizeInversion(rearrangement);
            case INSERTION:
                return prioritizeInsertion(rearrangement);
            case TRANSLOCATION:
                return prioritizeTranslocation(rearrangement);
            case DUPLICATION:
                return prioritizeDuplication(rearrangement);
        }
        // for development - TODO, figure out graceful default prioritization!
        LOGGER.error("Could not prioritize rearrangement with type=" + rearrangement.getType());
        return null;
        // throw new SvAnnRuntimeException("Could not prioritize rearrangement with type=" + rearrangement.getType());
    }

    private SvPriority prioritizeDuplication(SequenceRearrangement rearrangement) {
        // TODO: 2. 11. 2020 fix
        // rules for prioritizing duplication
        return DefaultSvPriority.unknown();
    }

    /**
     * Prioritize deletions according to sequence
     *
     * @param rearrangement The  deletion
     * @return Prioritization
     */
    private DefaultSvPriority prioritizeDeletion(SequenceRearrangement rearrangement) {
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .min(Comparator.comparing(Overlap::getOverlapType)); // counterintuitive but correct
        if (highestImpactOverlapOpt.isEmpty()) {
            // should never happen
            LOGGER.error("Could not identify highest impact overlap for {}.", rearrangement);
            return DefaultSvPriority.unknown();
        }
        Overlap highestImpactOverlap = highestImpactOverlapOpt.get();
        Set<String> affectedGeneIds = overlaps.stream().map(Overlap::getGeneSymbol).collect(Collectors.toSet());
        Set<GeneWithId> geneWithIdsSet = new HashSet<>();
        for (String symbol: affectedGeneIds) {
            if (geneSymbolMap.containsKey(symbol)) {
                geneWithIdsSet.add(geneSymbolMap.get(symbol));
            }
        }
        Set<TranscriptModel> affectedTranscripts =
                overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toSet());
        SvImpact impact = SvImpact.LOW_IMPACT; // default
        SvType svType = SvType.DELETION;
        OverlapType highestOT = highestImpactOverlap.getOverlapType();
        if (affectedGeneIds.size()>1) {
            impact = SvImpact.HIGH_IMPACT;
        } else if (highestOT.toImpact() == SvImpact.HIGH_IMPACT) {
            if (highestImpactOverlap.isOverlapsCds())
                impact = SvImpact.HIGH_IMPACT;
            else
                impact = SvImpact.INTERMEDIATE_IMPACT;
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size()>0) {
            impact = SvImpact.HIGH_IMPACT;
        }
        return new DefaultSvPriority(rearrangement,svType, impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }

    /**
     * The sequence-based inversion prioritization only checks whether the two breakends of the
     * inversion disrupt a gene. Here, we take any inversion that disrupts any part of a transcript
     * to be high impact (regardless of whether the CDS is disrupted).
     * @param rearrangement an inversion
     * @return Prioritization
     */
    private DefaultSvPriority prioritizeInversion(SequenceRearrangement rearrangement) {
        // the following gets overlaps of just the breakends of the inversion!
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        // if overlaps is not empty, then we regard this as high impact, other wise low
        SvImpact impact = SvImpact.LOW_IMPACT; // default
        OverlapType otype = OverlapType.UNKNOWN; // default
        Set<TranscriptModel> affectedTranscripts;
        Set<GeneWithId> geneWithIdsSet;
        if (! overlaps.isEmpty()) {
            Set<String> affectedGeneIds = overlaps.stream().map(Overlap::getGeneSymbol).collect(Collectors.toSet());
            geneWithIdsSet = new HashSet<>();
            for (String symbol: affectedGeneIds) {
                if (geneSymbolMap.containsKey(symbol)) {
                    geneWithIdsSet.add(geneSymbolMap.get(symbol));
                }
            }
            affectedTranscripts =
                    overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toSet());
            impact = SvImpact.HIGH_IMPACT;
            otype = OverlapType.TRANSCRIPT_DISRUPTED_BY_INVERSION;
        } else {
            affectedTranscripts = Set.of();
            geneWithIdsSet = Set.of();
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size()>0) {
            impact = SvImpact.HIGH_IMPACT;
        }
        return new DefaultSvPriority(rearrangement, SvType.INVERSION, impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }

    /**
     * Sequence based insertion prioritization. Any insertion that occurs in exonic regions is considered HIGH impact
     * except for insertions into UTR regions of coding genes (Any insertion in a transcript of a non-coding gene
     * is considered high impact).
     * @param rearrangement an insertion
     * @return Corresponding prioritization according to sequence
     */
    private DefaultSvPriority prioritizeInsertion(SequenceRearrangement rearrangement) {
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .min(Comparator.comparing(Overlap::getOverlapType)); // counterintuitive but correct
        if (highestImpactOverlapOpt.isEmpty()) {
            // should never happen
            LOGGER.error("Could not identify highest impact overlap for insertion: {}.", rearrangement);
            return DefaultSvPriority.unknown();
        }
        Overlap highestImpactOverlap = highestImpactOverlapOpt.get();
        Set<String> affectedGeneIds = overlaps.stream().map(Overlap::getGeneSymbol).collect(Collectors.toSet());
        Set<GeneWithId> geneWithIdsSet = new HashSet<>();
        for (String symbol: affectedGeneIds) {
            if (geneSymbolMap.containsKey(symbol)) {
                geneWithIdsSet.add(geneSymbolMap.get(symbol));
            }
        }
        Set<TranscriptModel> affectedTranscripts =
                overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toSet());
        SvImpact impact = SvImpact.LOW_IMPACT; // default
        OverlapType highestOT = highestImpactOverlap.getOverlapType();
        if (affectedGeneIds.size()>1) {
            impact = SvImpact.HIGH_IMPACT;
        } else if (highestOT.toImpact() == SvImpact.HIGH_IMPACT) {
            if (highestImpactOverlap.isOverlapsCds())
                impact = SvImpact.HIGH_IMPACT;
            else
                impact = SvImpact.INTERMEDIATE_IMPACT;
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size()>0) {
            impact = SvImpact.HIGH_IMPACT;
        }
        return new DefaultSvPriority(rearrangement,
                SvType.INSERTION,
                impact,
                affectedTranscripts,
                geneWithIdsSet,
                enhancers,
                overlaps);
    }

    private DefaultSvPriority prioritizeTranslocation(SequenceRearrangement rearrangement) {
        // the following gets overlaps that disrupt transcripts only
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        SvImpact impact = SvImpact.LOW_IMPACT; // default
        OverlapType otype = OverlapType.UNKNOWN; // default
        Set<TranscriptModel> affectedTranscripts;
        Set<GeneWithId> geneWithIdsSet;
        if (! overlaps.isEmpty()) {
            Set<String> affectedGeneIds = overlaps.stream().map(Overlap::getGeneSymbol).collect(Collectors.toSet());
            geneWithIdsSet = new HashSet<>();
            for (String symbol: affectedGeneIds) {
                if (geneSymbolMap.containsKey(symbol)) {
                    geneWithIdsSet.add(geneSymbolMap.get(symbol));
                }
            }
            affectedTranscripts =
                    overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toSet());
            impact = SvImpact.HIGH_IMPACT;
            otype = OverlapType.TRANSCRIPT_DISRUPTED_BY_INVERSION;
        } else {
            affectedTranscripts = Set.of();
            geneWithIdsSet = Set.of();
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size()>0) {
            impact = SvImpact.HIGH_IMPACT;
        }
        return new DefaultSvPriority(rearrangement,
                SvType.TRANSLOCATION,
                impact,
                affectedTranscripts,
                geneWithIdsSet,
                enhancers,
                overlaps);
    }

    @Override
    public SvPriority prioritize(SequenceRearrangement rearrangement) {
        return SvPriority.unknown();
    }
}
