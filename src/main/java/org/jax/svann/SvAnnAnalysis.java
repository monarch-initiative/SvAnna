package org.jax.svann;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.hpo.HpoDiseaseGeneMap;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.parse.*;
import org.jax.svann.priority.PrioritizedSv;
import org.jax.svann.priority.PrototypeSvPrioritizer;
import org.jax.svann.priority.SvPrioritizer;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TSpecParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SvAnnAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(SvAnnAnalysis.class);
    final static double THRESHOLD = 1;
    private final static int DISTANCE_THRESHOLD = 500_000;
    private final String prefix;
    private final List<TermId> targetHpoIdList;


    /** Key: A chromosome id; value: a Jannovar Interval array for searching for overlapping enhancers. */
    private final Map<Integer, IntervalArray<Enhancer>> chromosomeToEnhancerIntervalArrayMap;

    /**
     * This map is initialized to contain only those gene ids that are associated with diseases that
     * are annotated to at least one of the HPO terms in {@link #targetHpoIdList}.
     */
    private final Map<TermId, Set<HpoDiseaseSummary>> relevantGeneIdToAssociatedDiseaseMap;
    /** This set contains the HPO Term ids used as keys in {@link #relevantGeneIdToAssociatedDiseaseMap} that
     * also are equal to or ancestors of terms in {@link #targetHpoIdList}, i.e., that are phenotypically
     * relevant.
     */
    private final Set<TermId> relevantHpoIdsForEnhancers;

//    private final List<SvEvent> svEventList;
//
//    private final List<BreakendRecord> breakendRecordList;
    private final Collection<SequenceRearrangement> rearrangements;

    private final JannovarData jannovarData;

    /**
     * TODO allow as parameter to CTOR
     */
    private final GenomeAssembly assembly = GenomeAssemblyProvider.getGrch38Assembly();

    /**
     *
     * @param vcfFile path to a structural variant VCF file
     * @param prefix prefix for the output file
     * @param tidList list of relevant HPO terms
     * @param enhancerPath path to the TSpec enhancer file
     */
    public SvAnnAnalysis(String vcfFile, String prefix,  String enhancerPath, String jannovarPath, List<TermId> tidList) throws SerializationException {
        this.jannovarData = readJannovarData(jannovarPath);
        TSpecParser tparser = new TSpecParser(enhancerPath);
        Map<TermId, List<Enhancer>> id2enhancerMap = tparser.getId2enhancerMap();
        Map<TermId, String> hpoId2LabelMap = tparser.getId2labelMap();
        this.chromosomeToEnhancerIntervalArrayMap = tparser.getChromosomeToEnhancerIntervalArrayMap();
        HpoDiseaseGeneMap hpomap = HpoDiseaseGeneMap.loadGenesAndDiseaseMap();
        relevantGeneIdToAssociatedDiseaseMap = hpomap.getRelevantGenesAndDiseases(tidList);
        relevantHpoIdsForEnhancers = hpomap.getRelevantAncestors(id2enhancerMap.keySet(), tidList);
        this.prefix = prefix;
        this.targetHpoIdList = tidList;
        VcfStructuralRearrangementParser parser = new VcfStructuralRearrangementParser(assembly, new BreakendAssembler());
        Path vcfPath = Paths.get(vcfFile);
        try {
            rearrangements = parser.parseFile(vcfPath);
        } catch (IOException e) {
            throw new SvAnnRuntimeException("Error: " + e.getMessage());
        }
    }


    /**
     * Prioritize the structural variants in {@link #rearrangements}.
     * @return List of prioritized structural variants
     */
    public List<PrioritizedSv> prioritizeSvs() {
        List<PrioritizedSv> svList = new ArrayList<>();
        SvPrioritizer prioritizer = new PrototypeSvPrioritizer(assembly,
                relevantHpoIdsForEnhancers,
                chromosomeToEnhancerIntervalArrayMap,
                relevantGeneIdToAssociatedDiseaseMap,
                jannovarData);
        for (var rearrangement : rearrangements) {
            PrioritizedSv sv = prioritizer.prioritize(rearrangement);
            svList.add(sv);
        }
        return svList;
    }





    private static JannovarData readJannovarData(String jannovarDataPath) throws SerializationException {
        return new JannovarDataSerializer(jannovarDataPath).load();
    }


}
