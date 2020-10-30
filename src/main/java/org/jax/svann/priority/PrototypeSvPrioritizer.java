package org.jax.svann.priority;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class contains the code that we have written until now.
 */
public class PrototypeSvPrioritizer implements SvPrioritizer {

    private static final Set<HpoDiseaseSummary> EMPTYSET = Set.of();

    /**
     * Genome assembly in use.
     */
    private final GenomeAssembly assembly;

    /**
     * Collection of top level HPO ids that are equal to or ancestors of terms used to annotate the clinical
     * features of the individual being investigated.
     */
    private final Set<TermId> relevantHpoIdsForEnhancers;

    /**
     * Key: A chromosome id; value: a Jannovar Interval array for searching for overlapping enhancers.
     */
    private final Map<Integer, IntervalArray<Enhancer>> chromosomeToEnhancerIntervalArrayMap;
    private final Overlapper overlapper;

    private final EnhancerOverlapper enhancerOverlapper;

    /**
     * Reference dictionary (part of {@link JannovarData}).
     */
    private final ReferenceDictionary rd;

    /**
     * This map is initialized to contain only those gene ids that are associated with diseases that
     * are annotated to at least one of the HPO terms in targetHpoIdList.
     */
    private final Map<TermId, Set<HpoDiseaseSummary>> relevantGeneIdToAssociatedDiseaseMap;

    public PrototypeSvPrioritizer(GenomeAssembly assembly,
                                  Set<TermId> relevantHposForEnhancers,
                                  Map<Integer, IntervalArray<Enhancer>> enhancerMap,
                                  Map<TermId, Set<HpoDiseaseSummary>> gene2diseaseMap,
                                  JannovarData jannovarData) {
        this.assembly = assembly;
        this.relevantHpoIdsForEnhancers = relevantHposForEnhancers;
        this.chromosomeToEnhancerIntervalArrayMap = enhancerMap;
        this.relevantGeneIdToAssociatedDiseaseMap = gene2diseaseMap;
        this.overlapper = new Overlapper(jannovarData);
        this.enhancerOverlapper = new EnhancerOverlapper(jannovarData, enhancerMap);
        this.rd = jannovarData.getRefDict();
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
//                return prioritizeDeletion(rearrangement);
                break;
            case INVERSION:
//                return prioritizeInversion(rearrangement);
                break;
            case INSERTION:
//                return prioritizeInsertion(rearrangement);
                break;
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
                .filter(relevantGeneIdToAssociatedDiseaseMap::containsKey)
                .map(Overlap::getGeneSymbol)
                .collect(Collectors.toList());
        // TODO, we need to get a TermId, not a String. We may need to refactor relevantGeneIdToAssociatedDiseaseMap
        // to use the gene symbol as the key, since we will get potentially different gene ids from different
        // sources (NCBI, ucsc, Ensembl....)
        // Extract the relevant disease genes for this enhancer
        Set<HpoDiseaseSummary> diseases = relevantGeneIdToAssociatedDiseaseMap.getOrDefault("todo", EMPTYSET);
        //SequenceRearrangement e, List<Overlap> overlaps, List<Enhancer> enhancers, Set<HpoDiseaseSummary> diseases)
//        return new DefaultSvPriority(rearrangement, overlaps, affectedEnhancers, diseases);

        return null;
    }

    private DefaultSvPriority prioritizeDeletion(SequenceRearrangement rearrangement) {

        return null;
    }

    private DefaultSvPriority prioritizeInversion(SequenceRearrangement rearrangement) {
        return null;
    }

    private DefaultSvPriority prioritizeInsertion(SequenceRearrangement rearrangement) {
        return null;
    }

    private DefaultSvPriority prioritizeTranslocation(SequenceRearrangement rearrangement) {
        return null;
    }
}
