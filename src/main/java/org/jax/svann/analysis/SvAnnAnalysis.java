package org.jax.svann.analysis;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TSpecParser;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseGeneMap;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.parse.BreakendAssembler;
import org.jax.svann.parse.VcfStructuralRearrangementParser;
import org.jax.svann.priority.SvPrioritizer;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

// TODO: 2. 11. 2020 Should we move this functionality into the driver class?
public class SvAnnAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(SvAnnAnalysis.class);
    final static double THRESHOLD = 1;
    private final static int DISTANCE_THRESHOLD = 500_000;
    private final String prefix;
    /** HPO terms that characterize the individual whose VCF we are analyzing. */
    private final List<TermId> targetHpoIdList;
    /** Path to the VCF file to be analyzed. */
    private final String vcfPath;
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

    /**
     * Jannovar representation of all transcripts
     */
    private final JannovarData jannovarData;

    /**
     * Key -- gene symbol, vallue, {@link GeneWithId} object with symbol and id. This map is used to connect
     * geneIDs with gene symbols for display in HTML TODO - this is not very elegant and we may want to refactor.
     */
    private final Map<String, GeneWithId> geneSymbolMap;

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
        this.vcfPath = vcfFile;
        this.jannovarData = new JannovarDataSerializer(jannovarPath).load();
        TSpecParser tparser = new TSpecParser(enhancerPath);
        Map<TermId, List<Enhancer>> id2enhancerMap = tparser.getId2enhancerMap();
        Map<TermId, String> hpoId2LabelMap = tparser.getId2labelMap();
        this.chromosomeToEnhancerIntervalArrayMap = tparser.getChromosomeToEnhancerIntervalArrayMap();
        HpoDiseaseGeneMap hpomap = HpoDiseaseGeneMap.loadGenesAndDiseaseMap();
        relevantGeneIdToAssociatedDiseaseMap = hpomap.getRelevantGenesAndDiseases(tidList);
        this.geneSymbolMap = hpomap.getGeneSymbolMap();
        relevantHpoIdsForEnhancers = hpomap.getRelevantAncestors(id2enhancerMap.keySet(), tidList);
        this.prefix = prefix;
        this.targetHpoIdList = tidList;
    }

    /**
     * These are the prioritized objects that will be prioritized by {@link SvPrioritizer}-implementing
     * prioritizers that can be chosen by the user via the command line. The constructor of this class
     * puts all of the structural variants identified in the VCF file, both symbolic and breakend calls
     * into this list.
     *
     * @return List of sequence rearrangements from the VCF file before any prioritization
     */
    public Collection<SequenceRearrangement> getRawSequenceRearrangments() throws IOException {
        VcfStructuralRearrangementParser parser = new VcfStructuralRearrangementParser(assembly, new BreakendAssembler());
        Path vcfPath = Paths.get(this.vcfPath);
        return parser.parseFile(vcfPath);
    }


    /**
     * Prioritize the structural variants according to sequence
     * @param svList List of structural variants.
     * @return List of prioritized structural variants
     */
    public List<SvPriority> prioritizeSvsBySequence(List<SvPriority> svList) {
//        List<SvPriority> sequencePrioritized = new ArrayList<>();
//        SvPrioritizer prioritizer = new SequenceSvPrioritizer(assembly,
//                chromosomeToEnhancerIntervalArrayMap,
//                geneSymbolMap,
//                jannovarData);
//        for (var prio : svList) {
//            SvPriority prioritized = prioritizer.prioritize(prio);
//            if (prioritized == null) {
//                // TODO figure out any errors here
//                LOGGER.error("Not implemented: " + prio);
//                continue;
//            }
//            sequencePrioritized.add(prioritized);
//        }
//        return sequencePrioritized;
        return svList;
    }

    /**
     * Prioritize the structural variants according to phenotype
     * @param svList List of structural variants.
     * @return List of prioritized structural variants
     */
    public List<SvPriority> prioritizeSvsByPhenotype(List<SvPriority> svList) {
//        List<SvPriority> phenotypePrioritized = new ArrayList<>();
//        SvPrioritizer prioritizer = new PhenotypeSvPrioritizer(this.targetHpoIdList, this.relevantGeneIdToAssociatedDiseaseMap);
//        for (var prio : svList) {
//            SvPriority prioritized = prioritizer.prioritize(prio);
//            if (prioritized == null) {
//                // TODO figure out any errors here
//                LOGGER.error("Not implemented: " + prio);
//                continue;
//            }
//            phenotypePrioritized.add(prioritized);
//        }
//        return phenotypePrioritized;
        return svList;
    }
}
