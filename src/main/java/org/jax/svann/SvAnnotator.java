package org.jax.svann;

import com.google.common.collect.Multimap;
import org.jax.svann.lirical.LiricalHit;
import org.jax.svann.tspec.Enhancer;
import org.jax.svann.tspec.GencodeParser;
import org.jax.svann.tspec.TSpecParser;
import org.jax.svann.tspec.TssPosition;
import org.monarchinitiative.phenol.annotations.assoc.HpoAssociationParser;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This is the main class that coordinates parsing of the various input files to extract and prioritize structural
 * variants from long-read genome sequencing.
 * @author Peter Robinson
 */
public class SvAnnotator {

    final static double THRESHOLD = 1;
    /** The threshold for considering enhancers to be 'close' to target genes. */
    private final static int DISTANCE_THRESHOLD = 500_000;
    private final List<LiricalHit> hitlist;
    private final Map<TermId, Set<String>> diseaseId2GeneSymbolMap;
    private final String outputfile;
    private final List<TermId> targetHpoIdList;
    private final  Map<String, List<TssPosition>> symbolToTranscriptListMap;
    private final Set<Enhancer> phenotypicallyRelevantEnhancerSet = new HashSet<>();


    public SvAnnotator(String liricalPath, String Vcf, String outfile, List<TermId> tidList, String enhancerPath, String gencode) {

        TSpecParser tparser = new TSpecParser(enhancerPath);
        Map<TermId, List<Enhancer>> id2enhancerMap = tparser.getId2enhancerMap();
        Map<TermId, String> hpoId2LabelMap = tparser.getId2labelMap();
        GencodeParser gparser = new GencodeParser(gencode);
        this.symbolToTranscriptListMap = gparser.getSymbolToTranscriptListMap();
        this.diseaseId2GeneSymbolMap = initDiseaseMap();
        this.hitlist = new ArrayList<>();
        this.outputfile = outfile;
        this.targetHpoIdList = tidList;
        for (TermId t : tidList) {
            List<Enhancer> enhancers = id2enhancerMap.getOrDefault(t, new ArrayList<>());
            phenotypicallyRelevantEnhancerSet.addAll(enhancers);
        }
        for (var hit : hitlist) {
            Set<String> geneSymbols = hit.getGeneSymbols();
            Set<Enhancer> enhancers = getRelevantEnhancers(geneSymbols);
            hit.setEnhancerSet(enhancers);
        }
    }


    private Set<Enhancer>  getRelevantEnhancers(Set<String> geneSymbols) {
        Set<Enhancer> relevant = new HashSet<>();

        for (var gene : geneSymbols) {
            List<TssPosition> tssList = this.symbolToTranscriptListMap.getOrDefault(gene, List.of());
            for (var tss : tssList) {
                String chr = tss.getGenomicPosition().getChromosome();
                int pos = tss.getGenomicPosition().getPosition();
                for (var e : phenotypicallyRelevantEnhancerSet) {
                    if (e.matchesPos(chr, pos, DISTANCE_THRESHOLD)) {
                        relevant.add(e);
                    }
                }
            }
        }

        return relevant;
    }


    public SvAnnotator(String liricalPath, String Vcf, String outfile) {
        this.diseaseId2GeneSymbolMap = initDiseaseMap();
        symbolToTranscriptListMap = new HashMap<>();
        System.out.printf("We retrieved %d disease to gene annotations.\n", diseaseId2GeneSymbolMap.size());
        this.outputfile = outfile;
        hitlist = new ArrayList<>();

        System.out.println(Vcf);
        this.targetHpoIdList = null;
    }






    private Map<TermId, Set<String>> initDiseaseMap() {
        String geneinfo = "data/Homo_sapiens_gene_info.gz";
        String mimgene = "data/mim2gene_medgen";
        String hpo = "data/hp.obo";
        Ontology ontology = OntologyLoader.loadOntology(new File(hpo));
        HpoAssociationParser  parser = new HpoAssociationParser(geneinfo, mimgene, ontology);
        Map<TermId, String> id2sym = parser.getGeneIdToSymbolMap();
        Map<TermId, Set<String>> dis2symmap =  new HashMap<>();
        Multimap<TermId, TermId> mm = parser.getDiseaseToGeneIdMap();
        for (TermId diseaseId : mm.keys()) {
            dis2symmap.putIfAbsent(diseaseId, new HashSet<>());
            for (TermId geneId : mm.get(diseaseId)) {
                if (id2sym.containsKey(geneId)) {
                    dis2symmap.get(diseaseId).add(id2sym.get(geneId));
                }
            }
        }
        return dis2symmap;
    }
}
