package org.jax.svann.priority;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.HpoDiseaseSummary;
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

    private final GenomeAssembly assembly;
    /** Collection of top level HPO ids that are equal to or ancestors of terms used to annotate the clinical
     * features of the individual being investigated.
     */
    private final Set<TermId> relevantHpoIdsForEnhancers;


    /** Key: A chromosome id; value: a Jannovar Interval array for searching for overlapping enhancers. */
    private final Map<Integer, IntervalArray<Enhancer>> chromosomeToEnhancerIntervalArrayMap;

    private final Overlapper overlapper;

    /**
     * Reference dictionary (part of {@link JannovarData}).
     */
    private final ReferenceDictionary rd;

    private static final Set<HpoDiseaseSummary> EMPTYSET = Set.of();


    /**
     * This map is initialized to contain only those gene ids that are associated with diseases that
     * are annotated to at least one of the HPO terms in targetHpoIdList.
     */
    private final Map<TermId, Set<HpoDiseaseSummary>> relevantGeneIdToAssociatedDiseaseMap;

    PrototypeSvPrioritizer(GenomeAssembly assembly,
                           Set<TermId> relevantHposForEnhancers,
                           Map<Integer, IntervalArray<Enhancer>> enhancerMap,
                           Map<TermId, Set<HpoDiseaseSummary>> gene2diseaseMap,
                           JannovarData jannovarData) {
        this.assembly = assembly;
        this.relevantHpoIdsForEnhancers = relevantHposForEnhancers;
        this.chromosomeToEnhancerIntervalArrayMap = enhancerMap;
        this.relevantGeneIdToAssociatedDiseaseMap = gene2diseaseMap;
        overlapper = new Overlapper(jannovarData);
        this.rd = jannovarData.getRefDict();
    }


    @Override
    public PrioritizedSv prioritize(SequenceRearrangement rearrangement) {
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        // Here we want to do something fancy to see if an SV interrupts enhancer-gene interactions, e.g., Inversion,
        // but for now let's just see if the SV directly affects and enhancer
        for (var adjacency : rearrangement.getAdjacencies()) {
            int contigId = adjacency.getLeft().getContig().getId();
            GenomeInterval structVarInterval = new GenomeInterval(rd, Strand.FWD, contigId,
                    adjacency.getLeft().getBegin(),
                    adjacency.getRight().getEnd());
            // enhancer interval array does not work, we need method to contruct it from the intervals
           // IntervalArray<Enhancer> iarray = chromosomeToEnhancerIntervalArrayMap.get(contigId).g();
            //IntervalArray<Enhancer>.QueryResult queryResult =
            //         iarray.findOverlappingWithInterval(structVarInterval.getBeginPos(), structVarInterval.getEndPos());
        }
        List<Enhancer> affectedEnhancers = List.of();
        // TODO get list of genes located within XXX nucleotides of the affected enhancers.
        // TODO check if any of the genes in the "overlaps" or in the "affectedEnhancers" are
        // in  relevantGeneIdToAssociatedDiseaseMap -- if so we are PHENOTYPICALLY RELEVANT
        List<String> affectedRelevantGenes = overlaps
                .stream()
                .filter(g -> relevantGeneIdToAssociatedDiseaseMap.containsKey(g))
                .map(Overlap::getGeneSymbol)
                .collect(Collectors.toList());
        // TODO, we need to get a TermId, not a String. We may need to refactor relevantGeneIdToAssociatedDiseaseMap
        // to use the gene symbol as the key, since we will get potentially different gene ids from different
        // sources (NCBI, ucsc, Ensembl....)
        // Extract the relevant disease genes for this enhancer
        Set<HpoDiseaseSummary> diseases = relevantGeneIdToAssociatedDiseaseMap.getOrDefault("todo", EMPTYSET);
        //SequenceRearrangement e, List<Overlap> overlaps, List<Enhancer> enhancers, Set<HpoDiseaseSummary> diseases)
        return new PrioritizedSv(rearrangement, overlaps, affectedEnhancers, diseases);

    }


    private static JannovarData readJannovarData(String jannovarDataPath)  {
        try {
            JannovarData jdata = new JannovarDataSerializer(jannovarDataPath).load();
            return jdata;
        } catch (SerializationException e) {
            throw new SvAnnRuntimeException("Could not input Jannovar transcript data: " + e.getMessage());
        }
    }
}
