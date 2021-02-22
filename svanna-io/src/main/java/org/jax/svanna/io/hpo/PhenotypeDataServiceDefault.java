package org.jax.svanna.io.hpo;

import com.google.common.collect.Multimap;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoSubOntologyRootTermIds;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategory;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategoryMap;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.algo.InformationContentComputation;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.ResnikSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PhenotypeDataServiceDefault implements PhenotypeDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhenotypeDataServiceDefault.class);

    // HPO
    private final Ontology ontology;

    /**
     * Multimap from a gene id, e.g., NCBIGene:2200 for FBN1, and corresponding disease ids. In the case of
     * FBN1, this includes Marfan syndrome (OMIM:154700), Acromicric dysplasia (OMIM:102370), and others.
     */
    private final Multimap<TermId, TermId> diseaseToGeneMultiMap;

    private final Map<TermId, Set<TermId>> geneToDiseaseIdMap;

    /**
     * Map from disease IDs, e.g., OMIM:154700, to the corresponding HpoDisease object.
     */
    private final Map<TermId, HpoDisease> diseaseIdToDisease;

    private final Set<GeneWithId> geneWithIds;

    private final ResnikSimilarity resnikSimilarity;

    public PhenotypeDataServiceDefault(Ontology ontology,
                                       Multimap<TermId, TermId> diseaseToGeneMultiMap,
                                       Map<TermId, HpoDisease> diseaseIdToDisease,
                                       Set<GeneWithId> geneWithIds) {
        this.ontology = ontology;
        this.diseaseToGeneMultiMap = diseaseToGeneMultiMap;
        this.diseaseIdToDisease = diseaseIdToDisease;
        this.geneToDiseaseIdMap = prepareGeneToDiseaseMap(diseaseToGeneMultiMap);
        this.geneWithIds = geneWithIds;
        //TODO CHECK ME
        final Ontology phenotypicAbnormalitySubOntology = ontology.subOntology(HpoSubOntologyRootTermIds.PHENOTYPIC_ABNORMALITY);
        final InformationContentComputation icPrecomputation =
                new InformationContentComputation(phenotypicAbnormalitySubOntology);
        /** The TermId to object ID mapping. */
        final HashMap<TermId, Collection<TermId>> termIdToObjectId = new HashMap<>();
        final Map<TermId, Double> termToIc =
                icPrecomputation.computeInformationContent(termIdToObjectId);
        LOGGER.info("Done with precomputing information content.");

        LOGGER.info("Performing pairwise Resnik similarity precomputation...");
        this.resnikSimilarity =
                new ResnikSimilarity(
                        phenotypicAbnormalitySubOntology, termToIc, /* symmetric= */ false);
    }

    private Map<TermId, Set<TermId>> prepareGeneToDiseaseMap(Multimap<TermId, TermId> diseaseToGeneMultiMap) {
        Map<TermId, Set<TermId>> geneToDiseaseMap = new HashMap<>();

        for (Map.Entry<TermId, TermId> entry : diseaseToGeneMultiMap.entries()) {
            geneToDiseaseMap.putIfAbsent(entry.getValue(), new HashSet<>());
            geneToDiseaseMap.get(entry.getValue()).add(entry.getKey());
        }

        return geneToDiseaseMap;
    }


    @Override
    public Ontology ontology() {
        return ontology;
    }

    @Override
    public Set<GeneWithId> geneWithIds() {
        return geneWithIds;
    }


    public double resnikSimilarity(Set<TermId> hpoTerms, TermId geneId) {

        if (! this.geneToDiseaseIdMap.containsKey(geneId)) {
            return 0.0;
        }
        Set<TermId> diseaseIdSet = this.geneToDiseaseIdMap.get(geneId);
        double maxResnik = 0;
        TermId maxDisease = null;
        for (TermId diseaseId : diseaseIdSet) {
            HpoDisease disease = this.diseaseIdToDisease.get(diseaseId);
           // TODO -- make sure we only use positive annotations
            List<TermId> diseaseAnnotations = disease.getPhenotypicAbnormalityTermIdList();
            double sim =  resnikSimilarity.computeScore(diseaseAnnotations, hpoTerms);
            if (sim>maxResnik) {
                maxResnik = sim;
                maxDisease = diseaseId;
            }
        }

        return maxResnik;
    }



    @Override
    public Map<TermId, Set<HpoDiseaseSummary>> getRelevantGenesAndDiseases(Collection<TermId> hpoTermIds) {
        Map<TermId, Set<HpoDiseaseSummary>> builder = new HashMap<>();
        for (HpoDisease disease : diseaseIdToDisease.values()) {
            Collection<TermId> associatedGenes = this.diseaseToGeneMultiMap.get(disease.getDiseaseDatabaseId());
            if (associatedGenes.isEmpty()) {
                continue; // we are not interested in diseases with no associated genes for this prioritization
            }
            Set<TermId> totalAnnotations = OntologyAlgorithm.getAncestorTerms(ontology, Set.copyOf(disease.getPhenotypicAbnormalityTermIdList()), true);
            for (TermId tid : hpoTermIds) {
                if (totalAnnotations.contains(tid)) {
                    for (TermId geneId : associatedGenes) {
                        builder.putIfAbsent(geneId, new HashSet<>());
                        builder.get(geneId).add(HpoDiseaseSummary.of(disease.getDiseaseDatabaseId().getValue(), disease.getName()));
                    }
                }
            }
        }
        return Map.copyOf(builder);
    }

    @Override
    public Set<HpoDiseaseSummary> getDiseasesForGene(TermId gene) {
        if (!geneToDiseaseIdMap.containsKey(gene))
            return Set.of();
        return geneToDiseaseIdMap.get(gene).stream()
                .map(diseaseIdToDisease::get)
                .filter(Objects::nonNull)
                .filter(hpoDisease -> hpoDisease.getDiseaseDatabaseId() != null)
                .map(disease -> HpoDiseaseSummary.of(disease.getDiseaseDatabaseId().getValue(), disease.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Term> getTopLevelTerms(Collection<Term> hpoTermIds) {
        HpoCategoryMap catmap = new HpoCategoryMap();
        catmap.addAnnotatedTerms(hpoTermIds.stream().map(Term::getId).collect(Collectors.toList()), ontology);
        return catmap.getActiveCategoryList().stream()
                .map(HpoCategory::getTid)
                .filter(termId -> ontology.getTermMap().containsKey(termId))
                .map(termId -> ontology.getTermMap().get(termId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Term> validateTerms(Collection<TermId> hpoTermIds) {
        return hpoTermIds.stream()
                .filter(validateTerm())
                .map(termId -> ontology.getTermMap().get(termId))
                .collect(Collectors.toSet());
    }

    private Predicate<? super TermId> validateTerm() {
        return termId -> {
            if (!ontology.getTermMap().containsKey(termId)) {
                LogUtils.logWarn(LOGGER, "Term ID `{}` is not present in the used ontology", termId);
                return false;
            }
            return true;
        };
    }

    @Override
    public Set<TermId> getRelevantAncestors(Collection<TermId> candidates, Collection<TermId> hpoTermIds) {
        Set<TermId> ancs = OntologyAlgorithm.getAncestorTerms(ontology, new HashSet<>(hpoTermIds), true);
        return candidates.stream()
                .filter(ancs::contains)
                .collect(Collectors.toSet());
    }
}
