package org.jax.svanna.io.hpo;

import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategory;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategoryMap;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.data.TermIds;
import org.monarchinitiative.sgenes.model.GeneIdentifier;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Deprecated
public class PhenotypeDataServiceDefault implements PhenotypeDataService {

    // HPO
    private final Ontology ontology;

    private final Map<String, Set<TermId>> geneToDiseaseIdMap;

    /**
     * Map from disease IDs, e.g., OMIM:154700, to the corresponding HpoDisease object.
     */
    private final Map<String, HpoDisease> diseaseIdToDisease;

    private final Set<GeneIdentifier> geneIdentifiers;

    public PhenotypeDataServiceDefault(Ontology ontology,
                                       Map<TermId, Set<TermId>> diseaseToGeneMultiMap,
                                       Map<String, HpoDisease> diseaseIdToDisease,
                                       Set<GeneIdentifier> geneIdentifiers) {
        this.ontology = ontology;
        this.diseaseIdToDisease = diseaseIdToDisease;
        this.geneToDiseaseIdMap = prepareGeneToDiseaseMap(diseaseToGeneMultiMap);
        this.geneIdentifiers = geneIdentifiers;
    }

    private Map<String, Set<TermId>> prepareGeneToDiseaseMap(Map<TermId, Set<TermId>> diseaseToGeneMultiMap) {
        Map<String, Set<TermId>> builder = new HashMap<>(diseaseToGeneMultiMap.keySet().size());
        for (Map.Entry<TermId, Set<TermId>> entry : diseaseToGeneMultiMap.entrySet()) {
            for (TermId geneId : entry.getValue()) {
                String geneAccessionId = geneId.getValue();
                builder.putIfAbsent(geneAccessionId, new HashSet<>());
                builder.get(geneAccessionId).add(entry.getKey());
            }
        }

        Map<String, Set<TermId>> geneToDiseaseMap = new HashMap<>(builder.size());
        for (Map.Entry<String, Set<TermId>> entry : builder.entrySet()) {
            geneToDiseaseMap.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }

        return Collections.unmodifiableMap(geneToDiseaseMap);
    }


    @Override
    public Ontology ontology() {
        return ontology;
    }

    @Override
    public Stream<GeneIdentifier> geneWithIds() {
        return geneIdentifiers.stream();
    }


    @Override
    public List<HpoDiseaseSummary> getDiseasesForGene(String hgncId) {
        if (!geneToDiseaseIdMap.containsKey(hgncId))
            return List.of();
        return geneToDiseaseIdMap.get(hgncId).stream()
                .map(diseaseIdToDisease::get)
                .filter(Objects::nonNull)
                .filter(hpoDisease -> hpoDisease.getDiseaseDatabaseId() != null)
                .map(disease -> HpoDiseaseSummary.of(
                        disease.getDiseaseDatabaseId().getValue(),
                        disease.getDiseaseName()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<TermId> phenotypicAbnormalitiesForDiseaseId(String diseaseId) {
        HpoDisease disease = diseaseIdToDisease.get(diseaseId);
        if (disease == null)
            return List.of();

        // add term ancestors
        return List.copyOf(TermIds.augmentWithAncestors(ontology,
                Set.copyOf(disease.getPhenotypicAbnormalityTermIdList()),
                true));
    }

    @Override
    public Set<Term> getTopLevelTerms(Collection<Term> hpoTermIds) {
        HpoCategoryMap catmap = new HpoCategoryMap();
        catmap.addAnnotatedTerms(hpoTermIds.stream().map(Term::id).collect(Collectors.toList()), ontology);
        return catmap.getActiveCategoryList().stream()
                .map(HpoCategory::id)
                .filter(termId -> ontology.getTermMap().containsKey(termId))
                .map(termId -> ontology.getTermMap().get(termId))
                .collect(Collectors.toUnmodifiableSet());
    }

    /*
    private static List<ModeOfInheritance> parseModeOfInheritance(List<TermId> modesOfInheritance) {
        return modesOfInheritance.stream()
                .map(toModeOfInheritance())
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableList());
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
    */
}
