package org.jax.svanna.io.hpo;

import com.google.common.collect.Multimap;
import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.hpo.SimilarityScoreCalculator;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.ModeOfInheritance;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategory;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategoryMap;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ielis.silent.genes.model.GeneIdentifier;

import java.util.*;
import java.util.function.Function;
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

    private final Map<String, Set<TermId>> geneToDiseaseIdMap;

    /**
     * Map from disease IDs, e.g., OMIM:154700, to the corresponding HpoDisease object.
     */
    private final Map<TermId, HpoDisease> diseaseIdToDisease;

    private final Set<GeneIdentifier> geneIdentifiers;

    private final SimilarityScoreCalculator similarityScoreCalculator;

    public PhenotypeDataServiceDefault(Ontology ontology,
                                       Multimap<TermId, TermId> diseaseToGeneMultiMap,
                                       Map<TermId, HpoDisease> diseaseIdToDisease,
                                       Set<GeneIdentifier> geneIdentifiers,
                                       SimilarityScoreCalculator similarityScoreCalculator) {
        this.ontology = ontology;
        this.diseaseToGeneMultiMap = diseaseToGeneMultiMap;
        this.diseaseIdToDisease = diseaseIdToDisease;
        this.geneToDiseaseIdMap = prepareGeneToDiseaseMap(diseaseToGeneMultiMap);
        this.geneIdentifiers = geneIdentifiers;
        this.similarityScoreCalculator = similarityScoreCalculator;
    }

    private Map<String, Set<TermId>> prepareGeneToDiseaseMap(Multimap<TermId, TermId> diseaseToGeneMultiMap) {
        Map<String, Set<TermId>> geneToDiseaseMap = new HashMap<>();

        for (Map.Entry<TermId, TermId> entry : diseaseToGeneMultiMap.entries()) {
            String geneAccessionId = entry.getValue().getValue();
            geneToDiseaseMap.putIfAbsent(geneAccessionId, new HashSet<>());
            geneToDiseaseMap.get(geneAccessionId).add(entry.getKey());
        }

        return geneToDiseaseMap;
    }


    @Override
    public Ontology ontology() {
        return ontology;
    }

    @Override
    public Set<GeneIdentifier> geneWithIds() {
        return geneIdentifiers;
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
                        builder.get(geneId).add(
                                HpoDiseaseSummary.of(
                                        disease.getDiseaseDatabaseId().getValue(),
                                        disease.getName(),
                                        parseModeOfInheritance(disease.getModesOfInheritance())
                                ));
                    }
                }
            }
        }
        return Map.copyOf(builder);
    }

    @Override
    public Set<HpoDiseaseSummary> getDiseasesForGene(String accession) {
        if (!geneToDiseaseIdMap.containsKey(accession))
            return Set.of();
        return geneToDiseaseIdMap.get(accession).stream()
                .map(diseaseIdToDisease::get)
                .filter(Objects::nonNull)
                .filter(hpoDisease -> hpoDisease.getDiseaseDatabaseId() != null)
                .map(disease -> HpoDiseaseSummary.of(
                        disease.getDiseaseDatabaseId().getValue(),
                        disease.getName(),
                        parseModeOfInheritance(disease.getModesOfInheritance())))
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
    public Set<TermId> getRelevantAncestors(Collection<TermId> candidates, Collection<TermId> ancestorTerms) {
        Set<TermId> candidateAncestors = OntologyAlgorithm.getAncestorTerms(ontology, Set.copyOf(candidates), true);
        return candidateAncestors.stream()
                .filter(ancestorTerms::contains)
                .collect(Collectors.toSet());
    }

    @Override
    public double computeSimilarityScore(Collection<TermId> query, Collection<TermId> target) {
        return similarityScoreCalculator.computeSimilarityScore(query, target);
    }

    private static Set<ModeOfInheritance> parseModeOfInheritance(List<TermId> modesOfInheritance) {
        return modesOfInheritance.stream()
                .map(toModeOfInheritance())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    private static Function<TermId, Optional<ModeOfInheritance>> toModeOfInheritance() {
        return term -> {
            switch (term.getValue()) {
                case "HP:0000006": // Autosomal dominant inheritance
                case "HP:0001444": // Autosomal dominant somatic cell mutation
                case "HP:0012274": // Autosomal dominant inheritance with paternal imprinting
                case "HP:0012275": // Autosomal dominant inheritance with maternal imprinting
                case "HP:0025352": // Autosomal dominant germline de novo mutation
                    return Optional.of(ModeOfInheritance.AUTOSOMAL_DOMINANT);
                case "HP:0001417": // X-linked inheritance TODO - this might not be entirely correct binning
                case "HP:0001423": // X-linked dominant inheritance
                case "HP:0001470": // Sex-limited autosomal dominant
                case "HP:0001475": // Male-limited autosomal dominant
                    return Optional.of(ModeOfInheritance.X_DOMINANT);
                case "HP:0000007": // Autosomal recessive inheritance
                    return Optional.of(ModeOfInheritance.AUTOSOMAL_RECESSIVE);
                case "HP:0001419": // X-linked recessive inheritance
                case "HP:0031362": // Sex-limited autosomal recessive inheritance
                    return Optional.of(ModeOfInheritance.X_RECESSIVE);
                case "HP:0001427": // Mitochondrial inheritance
                    return Optional.of(ModeOfInheritance.MITOCHONDRIAL);
                case "HP:0001450": // Y-linked inheritance
                    return Optional.of(ModeOfInheritance.Y_LINKED);
                case "HP:0001425": // Heterogeneous
                case "HP:0001426": // Multifactorial inheritance
                case "HP:0001428": // Somatic mutation
                case "HP:0001442": // Somatic mosaicism
                case "HP:0003743": // Genetic anticipation
                case "HP:0003745": // Sporadic
                case "HP:0010982": // Polygenic inheritance
                case "HP:0010983": // Oligogenic inheritance
                case "HP:0010984": // Digenic inheritance
                case "HP:0032382": // Uniparental disomy
                case "HP:0001466": // Contiguous gene syndrome
                case "HP:0001452": // Autosomal dominant contiguous gene syndrome
                case "HP:0003744": // Genetic anticipation with paternal anticipation bias
                    return Optional.of(ModeOfInheritance.UNKNOWN);
                default:
                    LogUtils.logWarn(LOGGER, "Unknown mode of inheritance `{}`", term.getValue());
                    return Optional.of(ModeOfInheritance.UNKNOWN);
            }
        };
    }

}
