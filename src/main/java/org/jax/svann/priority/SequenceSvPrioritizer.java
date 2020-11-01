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


    @Override
    public SvPriority prioritize(SequenceRearrangement rearrangement) {
        switch (rearrangement.getType()) {
            case DELETION:
                return prioritizeDeletion(rearrangement);
            case INVERSION:
//                return prioritizeInversion(rearrangement);
                break;
            case INSERTION:
               return prioritizeInsertion(rearrangement);
            case TRANSLOCATION:
//                return prioritizeTranslocation(rearrangement);
                break;
        }

        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        // Here we want to do something fancy to see if an SV interrupts enhancer-gene interactions, e.g., Inversion,
        // but for now let's just see if the SV directly affects and enhancer

        List<Enhancer> affectedEnhancers = List.of();
        // TODO get list of genes located within XXX nucleotides of the affected enhancers.
        // TODO check if any of the genes in the "overlaps" or in the "affectedEnhancers" are
        // in  relevantGeneIdToAssociatedDiseaseMap -- if so we are PHENOTYPICALLY RELEVANT
        List<String> affectedRelevantGenes = overlaps
                .stream()
                //ilter(relevantGeneIdToAssociatedDiseaseMap::containsKey)
                .map(Overlap::getGeneSymbol)
                .collect(Collectors.toList());
        // TODO, we need to get a TermId, not a String. We may need to refactor relevantGeneIdToAssociatedDiseaseMap
        // to use the gene symbol as the key, since we will get potentially different gene ids from different
        // sources (NCBI, ucsc, Ensembl....)
        // Extract the relevant disease genes for this enhancer
       // Set<HpoDiseaseSummary> diseases = relevantGeneIdToAssociatedDiseaseMap.getOrDefault("todo", EMPTYSET);
        //SequenceRearrangement e, List<Overlap> overlaps, List<Enhancer> enhancers, Set<HpoDiseaseSummary> diseases)
//        return new DefaultSvPriority(rearrangement, overlaps, affectedEnhancers, diseases);

        return null;
    }

    /**
     * Prioritize deletions according to sequence
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
        return new DefaultSvPriority(svType, impact, affectedTranscripts, geneWithIdsSet, enhancers);
    }

    /**
     * The sequence-based inversion prioritization only checks whether the two breakends of the
     * inversion disrupt a gene. Here, we take any inversion that disrupts any part of a transcript
     * to be high impact (regardless of whether the CDS is disrupted).
     * @param rearrangement an inversion
     * @return Prioritization
     */
    private DefaultSvPriority prioritizeInversion(SequenceRearrangement rearrangement) {
        return null;
    }

    private DefaultSvPriority prioritizeInsertion(SequenceRearrangement rearrangement) {
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .min(Comparator.comparing(Overlap::getOverlapType)); // counterintuitive but correct
        if (highestImpactOverlapOpt.isEmpty()) {
            //
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
        SvType svType = SvType.INSERTION;
        OverlapType highestOT = highestImpactOverlap.getOverlapType();
        if (affectedGeneIds.size()>1) {
            impact = SvImpact.HIGH_IMPACT;
        } else if (highestOT.toImpact() == SvImpact.HIGH_IMPACT) {
            if (highestImpactOverlap.isOverlapsCds())
                impact = SvImpact.HIGH_IMPACT;
            else
                impact = SvImpact.INTERMEDIATE_IMPACT;
        }
        // TODO -- FIGURE OUT LOGIC FOR IMPACT

        // TODO -- Add Enhancers
        List<Enhancer> enhancers = List.of();
        // TODO -- calculate phenotypic relevance
        boolean hasPhenotypicRelevance = false;
        return new DefaultSvPriority(svType,
                impact,
                affectedTranscripts,
                geneWithIdsSet,
                enhancers,
                hasPhenotypicRelevance);
    }

    private DefaultSvPriority prioritizeTranslocation(SequenceRearrangement rearrangement) {
        return null;
    }
}
