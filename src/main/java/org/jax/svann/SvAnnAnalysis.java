package org.jax.svann;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.hpo.HpoDiseaseGeneMap;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.parse.BreakendRecord;
import org.jax.svann.parse.ParseResult;
import org.jax.svann.parse.SvEvent;
import org.jax.svann.parse.VcfStructuralVariantParser;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TSpecParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final List<SvEvent> svEventList;

    private final List<BreakendRecord> breakendRecordList;
    /** Object to find overlaps with transcripts. */
    private final Overlapper overlapper;

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
        JannovarData jannovarData = readJannovarData(jannovarPath);


        TSpecParser tparser = new TSpecParser(enhancerPath);
        Map<TermId, List<Enhancer>> id2enhancerMap = tparser.getId2enhancerMap();
        Map<TermId, String> hpoId2LabelMap = tparser.getId2labelMap();
        this.chromosomeToEnhancerIntervalArrayMap = tparser.getChromosomeToEnhancerIntervalArrayMap();
        HpoDiseaseGeneMap hpomap = HpoDiseaseGeneMap.loadGenesAndDiseaseMap();
        relevantGeneIdToAssociatedDiseaseMap = hpomap.getRelevantGenesAndDiseases(tidList);
        relevantHpoIdsForEnhancers = hpomap.getRelevantAncestors(id2enhancerMap.keySet(), tidList);
        VcfStructuralVariantParser vcfParser = new VcfStructuralVariantParser(assembly);
        Path vcfPath = Paths.get(vcfFile);
        ParseResult parseResult = vcfParser.parseFile(vcfPath);
        this.svEventList = parseResult.getAnns();
        this.breakendRecordList = parseResult.getBreakends();
        this.prefix = prefix;
        this.targetHpoIdList = tidList;
        overlapper = new Overlapper(jannovarData);
    }

    /**
     * TODO prototype
     */
    public void prioritizeSymbolSvs() {
        for (SvEvent sv : this.svEventList) {
            List<Overlap> overlaps = overlapper.getOverlapList(sv);
            // todo 1 create and store SvPriority objects
            //  do we store them as PrioritizedSv or as SvPriority?
            // todo 2 -- if overlaps does not have coding, then we could prioritize to a
            //  enhancer. We can use this -- chromosomeToEnhancerIntervalArrayMap
            //  we only need to find svs that disrupt enhancers and then they are prioritized based on
            //  the HPO term they contain
        }
    }

    public void prioritizeBreakendSvs() {
        // todo 1. merge BNDs that are pairs of the same adjancency together
        // todo 2. now or later, merge BNDs that have > 2 breakends, e.g. inversions together
        // todo 3. get overlaps
        // todo 4. create and store SvPriority objects
        //         do we store them as PrioritizedSv or as SvPriority?

        //  store them in the same list
    }


    public void prioritizeByPhenotype() {
        // todo 1. We can go over all of the PrioritizedSv objects created above
        // todo 2. Basically, we check whether
        //  a -- any of the affected genes are in relevantGeneIdToAssociatedDiseaseMap
        //   if so, add information about the gene and disease to the object
        //  b -- any of the affected enhancers have an HPO id in relevantHpoIdsForEnhancers
    }


    private static JannovarData readJannovarData(String jannovarDataPath) throws SerializationException {
        return new JannovarDataSerializer(jannovarDataPath).load();
    }


}
