package org.jax.l2o;

import com.google.common.collect.Multimap;
import org.jax.l2o.lirical.LiricalHit;
import org.jax.l2o.tspec.GencodeParser;
import org.jax.l2o.tspec.TSpecParser;
import org.jax.l2o.tspec.TssPosition;
import org.monarchinitiative.phenol.annotations.assoc.HpoAssociationParser;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Lirical2Overlap {

    final static double THRESHOLD = 1;
    private final List<LiricalHit> hitlist;
    private final Map<TermId, Set<String>> diseaseId2GeneSymbolMap;
    private final String outputfile;
    private final TermId targetHpoId;
   // private final List<TssPosition> tssPositions;



    public Lirical2Overlap(String liricalPath, String Vcf,  String outfile, TermId tid, String enhancerPath, String gencode) {

        TSpecParser tparser = new TSpecParser(enhancerPath);
        System.out.println("ABout to gparser");
        GencodeParser gparser = new GencodeParser(gencode);
       // List<TssPosition> getTranscripts()
        System.exit(1);
        this.diseaseId2GeneSymbolMap = initDiseaseMap();
        this.hitlist = new ArrayList<>();
        this.outputfile = outfile;
        this.targetHpoId = tid;

        init(liricalPath);


    }


    public Lirical2Overlap(String liricalPath, String Vcf,  String outfile) {
        this.diseaseId2GeneSymbolMap = initDiseaseMap();
        System.out.printf("We retrieved %d disease to gene annotations.\n", diseaseId2GeneSymbolMap.size());
        this.outputfile = outfile;
        hitlist = new ArrayList<>();

        System.out.println(Vcf);
        this.targetHpoId = null;
        init(liricalPath);
    }


    private void init(String liricalPath) {
        String line;
        File liricalFile = new File(liricalPath);
        if (! liricalFile.exists()) {
            System.err.printf("[ERROR] Could not find LIRICAL input file at %s\n", liricalPath);
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(liricalPath))) {
            //rank    diseaseName     diseaseCurie    pretestprob     posttestprob    compositeLR     entrezGeneId    variants
            while ((line=br.readLine()) != null) {
                if (line.startsWith("rank")) break;
            }
            while ((line=br.readLine()) != null) {
                String []fields = line.split("\t");
                if (fields.length < 8) {
                    System.err.println("[ERROR] Bad line, less than 8 fields: " + line);
                    continue;
                }
                String dname = fields[1];
                String dcurie = fields[2];
                String posttestprob = fields[4].replace("%","");
                try {
                    double prob = Double.parseDouble(posttestprob);
                    double lr = Double.parseDouble(fields[5].replaceAll(",",""));
                    if (prob > THRESHOLD) {
                        LiricalHit hit = new LiricalHit(dname, dcurie, prob, lr);
                        TermId diseaseId = TermId.of(dcurie);
                        if (this.diseaseId2GeneSymbolMap.containsKey(diseaseId)) {
                            //System.out.println("found genes for " + dname);
                            hit.setGeneSymbols(diseaseId2GeneSymbolMap.get(diseaseId));
                        }
                        hitlist.add(hit);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[ERROR] " + e.getLocalizedMessage());
                    System.err.println("[ERROR] " + line);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        System.out.printf("[INFO] We got %d above threshold candidates.\n", hitlist.size());


    }

    public List<LiricalHit> getHitlist() {
        return hitlist;
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
