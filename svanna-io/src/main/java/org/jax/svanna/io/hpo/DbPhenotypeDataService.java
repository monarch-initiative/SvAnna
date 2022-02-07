package org.jax.svanna.io.hpo;

import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategory;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategoryMap;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.sgenes.model.GeneIdentifier;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DbPhenotypeDataService implements PhenotypeDataService {

    private final Ontology ontology;
    private final List<GeneIdentifier> geneIdentifiers;
    private final Map<String, List<HpoDiseaseSummary>> hgncGeneIdToDisease;
    private final Map<String, List<TermId>> phenotypicAbnormalitiesForDiseaseId;

    public DbPhenotypeDataService(Ontology ontology,
                                  List<GeneIdentifier> geneIdentifiers,
                                  Map<String, List<HpoDiseaseSummary>> hgncGeneIdToDisease,
                                  Map<String, List<TermId>> phenotypicAbnormalitiesForDiseaseId) {
        this.ontology = Objects.requireNonNull(ontology, "Ontology must not be null");
        this.geneIdentifiers = Objects.requireNonNull(geneIdentifiers, "Gene identifiers must not be null");
        this.hgncGeneIdToDisease = Objects.requireNonNull(hgncGeneIdToDisease, "HGNC gene ID to diseases must not be null");
        this.phenotypicAbnormalitiesForDiseaseId = Objects.requireNonNull(phenotypicAbnormalitiesForDiseaseId, "Phenotypic abnormalities for disease ID must not be null");
    }

    @Override
    public Set<Term> getTopLevelTerms(Collection<Term> hpoTermIds) {
        HpoCategoryMap catmap = new HpoCategoryMap();
        Ontology ontology = ontology();
        catmap.addAnnotatedTerms(hpoTermIds.stream().map(Term::id).collect(Collectors.toList()), ontology);
        return catmap.getActiveCategoryList().stream()
                .map(HpoCategory::id)
                .filter(termId -> ontology.getTermMap().containsKey(termId))
                .map(termId -> ontology.getTermMap().get(termId))
                .collect(Collectors.toUnmodifiableSet());
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
        return hgncGeneIdToDisease.getOrDefault(hgncId, List.of());
    }

    @Override
    public List<TermId> phenotypicAbnormalitiesForDiseaseId(String diseaseId) {
        return phenotypicAbnormalitiesForDiseaseId.getOrDefault(diseaseId, List.of());
    }

}
