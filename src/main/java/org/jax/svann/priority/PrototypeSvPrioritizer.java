package org.jax.svann.priority;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.overlap.OverlapType;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.reference.SequenceRearrangement;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class PrototypeSvPrioritizer implements SvPrioritizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrototypeSvPrioritizer.class);

    /**
     * In the first pass, we just look at sequence and do not analyze diseases, so this is the empty set.
     */
    private static final Set<HpoDiseaseSummary> EMPTYSET = Set.of();

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
     * Collection of top level HPO ids that are equal to or ancestors of terms used to annotate the clinical
     * features of the individual being investigated.
     */
    private final Set<TermId> relevantHpoIdsForEnhancers;

    private final Set<TermId> patientPhenotypeTerms;

    /**
     * @param geneSymbolMap              A map of gene symbols and NCBI ids
     * @param relevantHpoIdsForEnhancers relevant terms to use for enhancer filtering
     */
    public PrototypeSvPrioritizer(Map<String, GeneWithId> geneSymbolMap,
                                  Overlapper overlapper,
                                  EnhancerOverlapper enhancerOverlapper,
                                  Set<TermId> patientPhenotypeTerms,
                                  Set<TermId> relevantHpoIdsForEnhancers) {
        this.geneSymbolMap = geneSymbolMap;
        this.overlapper = overlapper;
        this.enhancerOverlapper = enhancerOverlapper;
        this.patientPhenotypeTerms = patientPhenotypeTerms;
        this.relevantHpoIdsForEnhancers = relevantHpoIdsForEnhancers;
    }

    @Override
    public SvPriority prioritize(SequenceRearrangement rearrangement) {
        /*
        For each case we need to figure out the affected transcripts/genes.
        Then, we figure out the impact on the sequence (high/medium/low) and phenotype relevance.

        Sequence impact category:
        - use overlapper to get the overlapping transcripts
        - if overlap type is intergenic:
          - if variant disrupts an enhancer with tissue relevance -> medium sequence impact
            - else low
        - if overlap type is single/multiple/entire tx affected -> high sequence impact
        report the most severe sequence impact category

        Phenotypic relevance:
        - the affected gene is among the relevant genes -> relevant
        - translocation that removes enhancer from a phenotypically relevant gene
        - else not

        Note that the rules for getting "overlap" depends on SvType
         */
        switch (rearrangement.getType()) {
            case DELETION:
                return prioritizeDeletion(rearrangement);
            case INSERTION:
                return prioritizeInsertion(rearrangement);
            case INVERSION:
                return prioritizeInversion(rearrangement);
            case TRANSLOCATION:
                return prioritizeTranslocation(rearrangement);
            case DUPLICATION:
                return prioritizeDuplication(rearrangement);
            default:
                LOGGER.warn("Prioritization of {} is not yet supported", rearrangement.getType());
                return SvPriority.unknown();
        }
    }

    private SvPriority prioritizeDeletion(SequenceRearrangement rearrangement) {
        // find the gene/transcript with the most deleterious OverlapType
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        Optional<Overlap> highestImpactOverlapOpt = overlaps.stream()
                .max(Comparator.comparing(Overlap::getOverlapType));
        if (highestImpactOverlapOpt.isEmpty()) {
            // should never happen
            LOGGER.error("Could not identify highest impact overlap for {}.", rearrangement);
            return DefaultSvPriority.unknown();
        }
        Overlap highestImpactOverlap = highestImpactOverlapOpt.get();

        // select the relevant genes
        Set<String> affectedGeneIds = overlaps.stream().map(Overlap::getGeneSymbol).collect(Collectors.toSet());
        Set<GeneWithId> geneWithIdsSet = new HashSet<>();
        for (String symbol : affectedGeneIds) {
            if (geneSymbolMap.containsKey(symbol)) {
                geneWithIdsSet.add(geneSymbolMap.get(symbol));
            }
        }
        Set<TranscriptModel> affectedTranscripts = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toSet());
        SvImpact impact = SvImpact.LOW; // default

        OverlapType highestOT = highestImpactOverlap.getOverlapType();
        if (affectedGeneIds.size() > 1) {
            impact = SvImpact.HIGH;
        } else if (highestOT.toImpact() == SvImpact.HIGH) {
            if (highestImpactOverlap.isOverlapsCds())
                impact = SvImpact.HIGH;
            else
                impact = SvImpact.INTERMEDIATE;
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size() > 0) {
            impact = SvImpact.HIGH;
        }
        return new DefaultSvPriority(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }

    /**
     * Sequence based insertion prioritization. Any insertion that occurs in exonic regions is considered HIGH impact
     * except for insertions into UTR regions of coding genes (Any insertion in a transcript of a non-coding gene
     * is considered high impact).
     *
     * @param rearrangement an insertion
     * @return Corresponding prioritization according to sequence
     */
    private SvPriority prioritizeInsertion(SequenceRearrangement rearrangement) {
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
        for (String symbol : affectedGeneIds) {
            if (geneSymbolMap.containsKey(symbol)) {
                geneWithIdsSet.add(geneSymbolMap.get(symbol));
            }
        }
        Set<TranscriptModel> affectedTranscripts =
                overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toSet());
        SvImpact impact = SvImpact.LOW; // default
        OverlapType highestOT = highestImpactOverlap.getOverlapType();
        if (affectedGeneIds.size() > 1) {
            impact = SvImpact.HIGH;
        } else if (highestOT.toImpact() == SvImpact.HIGH) {
            if (highestImpactOverlap.isOverlapsCds())
                impact = SvImpact.HIGH;
            else
                impact = SvImpact.INTERMEDIATE;
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size() > 0) {
            impact = SvImpact.HIGH;
        }
        return new DefaultSvPriority(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }

    /**
     * The sequence-based inversion prioritization only checks whether the two breakends of the
     * inversion disrupt a gene. Here, we take any inversion that disrupts any part of a transcript
     * to be high impact (regardless of whether the CDS is disrupted).
     *
     * @param rearrangement an inversion
     * @return Prioritization
     */
    private SvPriority prioritizeInversion(SequenceRearrangement rearrangement) {
        // the following gets overlaps of just the breakends of the inversion!
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        // if overlaps is not empty, then we regard this as high impact, other wise low
        SvImpact impact = SvImpact.LOW; // default
        OverlapType otype = OverlapType.UNKNOWN; // default
        Set<TranscriptModel> affectedTranscripts;
        Set<GeneWithId> geneWithIdsSet;
        if (!overlaps.isEmpty()) {
            Set<String> affectedGeneIds = overlaps.stream().map(Overlap::getGeneSymbol).collect(Collectors.toSet());
            geneWithIdsSet = new HashSet<>();
            for (String symbol : affectedGeneIds) {
                if (geneSymbolMap.containsKey(symbol)) {
                    geneWithIdsSet.add(geneSymbolMap.get(symbol));
                }
            }
            affectedTranscripts =
                    overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toSet());
            impact = SvImpact.HIGH;
            otype = OverlapType.TRANSCRIPT_DISRUPTED_BY_INVERSION;
        } else {
            affectedTranscripts = Set.of();
            geneWithIdsSet = Set.of();
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size() > 0) {
            impact = SvImpact.HIGH;
        }
        return new DefaultSvPriority(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }


    private DefaultSvPriority prioritizeTranslocation(SequenceRearrangement rearrangement) {
        // the following gets overlaps that disrupt transcripts only
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        SvImpact impact = SvImpact.LOW; // default
        OverlapType otype = OverlapType.UNKNOWN; // default
        Set<TranscriptModel> affectedTranscripts;
        Set<GeneWithId> geneWithIdsSet;
        if (!overlaps.isEmpty()) {
            Set<String> affectedGeneIds = overlaps.stream().map(Overlap::getGeneSymbol).collect(Collectors.toSet());
            geneWithIdsSet = new HashSet<>();
            for (String symbol : affectedGeneIds) {
                if (geneSymbolMap.containsKey(symbol)) {
                    geneWithIdsSet.add(geneSymbolMap.get(symbol));
                }
            }
            affectedTranscripts =
                    overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toSet());
            impact = SvImpact.HIGH;
            otype = OverlapType.TRANSCRIPT_DISRUPTED_BY_INVERSION;
        } else {
            affectedTranscripts = Set.of();
            geneWithIdsSet = Set.of();
        }
        List<Enhancer> enhancers = enhancerOverlapper.getEnhancerOverlaps(rearrangement);
        if (enhancers.size() > 0) {
            impact = SvImpact.HIGH;
        }
        return new DefaultSvPriority(impact, affectedTranscripts, geneWithIdsSet, enhancers, overlaps);
    }

    private SvPriority prioritizeDuplication(SequenceRearrangement rearrangement) {
        // TODO: 2. 11. 2020 implement
        return SvPriority.unknown();
    }

}
