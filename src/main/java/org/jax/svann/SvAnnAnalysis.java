package org.jax.svann;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TSpecParser;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseGeneMap;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.parse.BreakendAssembler;
import org.jax.svann.parse.VcfStructuralRearrangementParser;
import org.jax.svann.priority.*;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.jax.svann.viz.HtmlVisualizable;
import org.jax.svann.viz.Visualizable;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SvAnnAnalysis {
    private static final Logger LOGGER = LoggerFactory.getLogger(SvAnnAnalysis.class);
    final static double THRESHOLD = 1;
    private final static int DISTANCE_THRESHOLD = 500_000;
    private final String prefix;
    private final List<TermId> targetHpoIdList;
    /**
     * TODO do we want to refactor @ielis?
     * This map includes counts of SVs that we do not display in detail in the HTML output.
     */
    private final Map<String, Integer> countsMap;


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
     * These are the prioritized objects that will be prioritized by {@link SvPrioritizer}-implementing
     * prioritizers that can be chosen by the user via the command line. The constructor of this class
     * puts all of the structural variants identified in the VCF file, both symbolic and breakend calls
     * into this list.
     */
    private final List<SvPriority> prioritizedStructuralVariants;
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
        VcfStructuralRearrangementParser parser = new VcfStructuralRearrangementParser(assembly, new BreakendAssembler());
        Path vcfPath = Paths.get(vcfFile);
        this.countsMap = new HashMap<>();
        try {
            Collection<SequenceRearrangement> rearrangements = parser.parseFile(vcfPath);
            this.prioritizedStructuralVariants = rearrangements.
                            stream().
                            map(DefaultSvPriority::createBaseSvPriority).
                            collect(Collectors.toList());
        } catch (IOException e) {
            throw new SvAnnRuntimeException("Error: " + e.getMessage());
        }
    }


    /**
     * Prioritize the structural variants in {@link #prioritizedStructuralVariants}.
     *
     * @return List of prioritized structural variants
     */
    public List<Visualizable> prioritizeSvs() {
        List<Visualizable> visualizableList = new ArrayList<>();
        SvPrioritizer prioritizer = new SequenceSvPrioritizer(assembly,
                chromosomeToEnhancerIntervalArrayMap,
                geneSymbolMap,
                jannovarData);
        boolean usePhenotype = false;
        PhenotypeSvPrioritizer phenoPrioritizer;
        if (this.targetHpoIdList.size() > 0) {
            usePhenotype = true;
            phenoPrioritizer = new PhenotypeSvPrioritizer();
        }
        for (var prio : prioritizedStructuralVariants) {
            SvPriority prioritized = prioritizer.prioritize(prio);
            if (prioritized == null) {
                this.countsMap.putIfAbsent("not.implemented", 0);
                this.countsMap.merge("not.implemented", 1, Integer::sum);
                continue;
            }
            if (usePhenotype) {
                // what
            }
            Visualizable visualizable = new HtmlVisualizable(prioritized);
            visualizableList.add(visualizable);
        }
        return visualizableList;
    }

}
