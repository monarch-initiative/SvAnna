package org.jax.svann.hpo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.jax.svann.except.SvAnnRuntimeException;
import org.monarchinitiative.phenol.annotations.assoc.HpoAssociationParser;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.obo.hpo.HpoDiseaseAnnotationParser;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.util.*;

/**
 * SvAnn can optionally prioritize structural variants that affect genes associated with diseases that are
 * characterized by a list of HPO terms passed by the user. To do so, we first input HPO-information about
 * phenotypes and diseases and gene associations. Then, we extract data about genes and diseases that are relevant
 * for the HPO terms.
 * @author Peter N Robinson
 */
public class HpoDiseaseGeneMap {
    /** Human Phenotype Ontology */
    private final Ontology ontology;
    /** Multimap from a gene id, e.g., NCBIGene:2200 for FBN1, and corresponding disease ids. In the case of
     * FBN1, this includes Marfan syndrome (OMIM:154700), Acromicric dysplasia (OMIM:102370), and others.
     */
    private final Multimap<TermId, TermId> disease2geneIdMultiMap;
    /**
     * Map from disease IDs, e.g., OMIM:154700, to the corresponding HpoDisease object.
     */
    private final Map<TermId, HpoDisease> diseaseMap;

    private HpoDiseaseGeneMap(String hpOboPath, String phenotypeHpoaPath, String mim2geneMedgenPath, String geneInfoPath) {
        this.ontology = OntologyLoader.loadOntology(new File(hpOboPath));
        List<String> desiredDatabasePrefixes= ImmutableList.of("OMIM");
        String orphaToGeneFile = null; // OK, this will not cause a crash, we will refactor in phenol
        HpoAssociationParser hap = new HpoAssociationParser(geneInfoPath, mim2geneMedgenPath, null, phenotypeHpoaPath, ontology);
        this.disease2geneIdMultiMap = hap.getDiseaseToGeneIdMap();

        this.diseaseMap = HpoDiseaseAnnotationParser.loadDiseaseMap(phenotypeHpoaPath,ontology,desiredDatabasePrefixes);
    }

    /**
     * Create a map with key=GeneId, value=Set of {@link HpoDiseaseSummary} objects corresponding to the gene.
     * We create entries for diseases associated with the target term ids.
     * @param targetHpoTermIds HPO ids entered by the user
     * @return map with key=GeneId, value=Set of {@link HpoDiseaseSummary} objects
     */
    public Map<TermId, Set<HpoDiseaseSummary>> getRelevantGenesAndDiseases(List<TermId> targetHpoTermIds){
        Map<TermId, Set<HpoDiseaseSummary>> gene2diseaseMap = new HashMap<>();
        for (var disease: diseaseMap.values()) {
            Collection<TermId> associatedGenes = this.disease2geneIdMultiMap.get(disease.getDiseaseDatabaseId());
            if (associatedGenes.isEmpty()) {
                continue; // we are not interested in diseases with no associated genes for this prioritization
            }
            List<TermId> directAnnotations = disease.getPhenotypicAbnormalityTermIdList();
            Set<TermId> totalAnnotations =
                    OntologyAlgorithm.getAncestorTerms( ontology, new HashSet<>(directAnnotations), true);
            for (TermId tid : targetHpoTermIds) {
                if (totalAnnotations.contains(tid)) {
                    for (TermId geneId : associatedGenes) {
                        gene2diseaseMap.putIfAbsent(geneId, new HashSet<>());
                        gene2diseaseMap.get(geneId).add(new HpoDiseaseSummary(disease, targetHpoTermIds));
                    }
                }
            }
        }
        return Map.copyOf(gene2diseaseMap); // immutable
    }

    /**
     * Entry point to using this class.
     * @param targetHpoTermIds List of terms provided by the user.
     * @return Map of genes/diseases characterized by one or more of these terms.
     */
    public static Map<TermId, Set<HpoDiseaseSummary>> loadRelevantGenesAndDiseases(List<TermId> targetHpoTermIds) {
        String hpoPath = String.join("data", File.separator, "hp.obo");
        String phenotypeHpoaPath = String.join("data", File.separator, "phenotype.hpoa");
        String mim2geneMedgenPath = String.join("data", File.separator, "mim2gene_medgen");
        String geneInfoPath = String.join("data", File.separator, "Homo_sapiens_gene_info.gz");
        File hpoFile = new File(hpoPath);
        File phenotypeFile = new File(phenotypeHpoaPath);
        File mim2geneFile = new File(mim2geneMedgenPath);
        File geneInfoFile = new File(geneInfoPath);
        if (! hpoFile.exists()) {
            throw new SvAnnRuntimeException("Could not find hp.obo. Did you run the download command?");
        }
        if (! phenotypeFile.exists()) {
            throw new SvAnnRuntimeException("Could not find phenotype.hpoa. Did you run the download command?");
        }
        if (! mim2geneFile.exists()) {
            throw new SvAnnRuntimeException("Could not find mim2gene_medgen Did you run the download command?");
        }
        if (! geneInfoFile.exists()) {
            throw new SvAnnRuntimeException("Could not find Homo_sapiens_gene_info.gz. Did you run the download command?");
        }
        HpoDiseaseGeneMap hdgmap = new HpoDiseaseGeneMap(hpoFile.getAbsolutePath(),
                phenotypeFile.getAbsolutePath(),
                mim2geneFile.getAbsolutePath(),
                geneInfoFile.getAbsolutePath());
        return hdgmap.getRelevantGenesAndDiseases(targetHpoTermIds);

    }

}
