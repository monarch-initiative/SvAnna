package org.jax.svann;

import org.jax.svann.hpo.HpoDiseaseGeneMap;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.parse.BreakendRecord;
import org.jax.svann.parse.ParseResult;
import org.jax.svann.parse.SvEvent;
import org.jax.svann.parse.VcfStructuralVariantParser;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TSpecParser;
import org.jax.svann.genomicreg.TssPosition;
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
    private final  Map<String, List<TssPosition>> symbolToTranscriptListMap;
    private final Set<Enhancer> phenotypicallyRelevantEnhancerSet = new HashSet<>();
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
     * TODO allow as parameter to CTOR
     */
    private final GenomeAssembly assembly = GenomeAssemblyProvider.getGrch38Assembly();

    /**
     *
     * @param Vcf path to a structural variant VCF file
     * @param prefix prefix for the output file
     * @param tidList list of relevant HPO terms
     * @param enhancerPath path to the TSpec enhancer file
     */
    public SvAnnAnalysis(String Vcf, String prefix, List<TermId> tidList, String enhancerPath) {

        TSpecParser tparser = new TSpecParser(enhancerPath);
        Map<TermId, List<Enhancer>> id2enhancerMap = tparser.getId2enhancerMap();
        Map<TermId, String> hpoId2LabelMap = tparser.getId2labelMap();
        HpoDiseaseGeneMap hpomap = HpoDiseaseGeneMap.loadGenesAndDiseaseMap();
        relevantGeneIdToAssociatedDiseaseMap = hpomap.getRelevantGenesAndDiseases(tidList);
        relevantHpoIdsForEnhancers = hpomap.getRelevantAncestors(id2enhancerMap.keySet(), tidList);
        symbolToTranscriptListMap = Map.of(); // TODO Populate from Jannovar
        VcfStructuralVariantParser vcfParser = new VcfStructuralVariantParser(assembly);
        Path vcfPath = Paths.get(Vcf);
        ParseResult parseResult = vcfParser.parseFile(vcfPath);
        List<SvEvent> svEventList = parseResult.getAnns();
        List<BreakendRecord> breakendRecordList = parseResult.getBreakends();
        this.prefix = prefix;
        this.targetHpoIdList = tidList;
        for (TermId t : tidList) {
            List<Enhancer> enhancers = id2enhancerMap.getOrDefault(t, new ArrayList<>());
            phenotypicallyRelevantEnhancerSet.addAll(enhancers);
        }
//        List<LiricalHit> hitlist = getLiricalHitList(liricalPath);
//        for (var hit : hitlist) {
//            Set<String> geneSymbols = hit.getGeneSymbols();
//            Set<Enhancer> enhancers = getRelevantEnhancers(geneSymbols);
//            hit.setEnhancerSet(enhancers);
//        }
    }

    /**
     * This constructor should be chosen if the user does not pass any HPO terms. In this case, the
     * prioritization is performed solely with sequence and transcript information.
     * @param Vcf path to a structural-variant VCF file
     * @param prefix prefix for output files
     */
    public SvAnnAnalysis(String Vcf, String prefix) {
        symbolToTranscriptListMap = new HashMap<>();
        this.prefix = prefix;
        relevantGeneIdToAssociatedDiseaseMap = Map.of();
        relevantHpoIdsForEnhancers = Set.of();
        System.out.println(Vcf);
        this.targetHpoIdList = null;

    }




    private Set<Enhancer>  getRelevantEnhancers(Set<String> geneSymbols) {
        Set<Enhancer> relevant = new HashSet<>();

        for (var gene : geneSymbols) {
            List<TssPosition> tssList = this.symbolToTranscriptListMap.getOrDefault(gene, List.of());
            for (var tss : tssList) {
                Contig chr = tss.getGenomicPosition().getChromosome();
                final Optional<Contig> contigOptional = assembly.getContigByName(chr.getPrimaryName());
                if (contigOptional.isEmpty()) {
                    LOGGER.warn("Unknown contig `{}` for {}", chr, tss);
                    continue;
                }
                final Contig contig = contigOptional.get();
                Position pos = tss.getGenomicPosition().getPosition();
                for (var e : phenotypicallyRelevantEnhancerSet) {
                    // TODO Do we need the TSS class and should it return a Position?
                    if (e.matchesPos(contig, pos , DISTANCE_THRESHOLD)) {
                        relevant.add(e);
                    }
                }
            }
        }

        return relevant;
    }



}
