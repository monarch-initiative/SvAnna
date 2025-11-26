package org.monarchinitiative.svanna.io.hpo;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategories;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategoryLookup;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svanna.core.service.PhenotypeDataService;
import org.monarchinitiative.svanna.model.HpoDiseaseSummary;

import java.util.*;
import java.util.stream.Collectors;

public class DbPhenotypeDataService implements PhenotypeDataService {

    private final MinimalOntology hpo;
    private final HpoDiseases hpoDiseases;
    private final Map<TermId, Collection<TermId>> geneIdToDiseaseIds;
    private final HpoCategoryLookup lookup;

    public DbPhenotypeDataService(MinimalOntology hpo,
                                  HpoDiseases hpoDiseases,
                                  Map<TermId, Collection<TermId>> geneIdToDiseaseIds
    ) {
        this.hpo = Objects.requireNonNull(hpo, "Ontology must not be null");
        this.hpoDiseases = Objects.requireNonNull(hpoDiseases);
        this.geneIdToDiseaseIds = Objects.requireNonNull(geneIdToDiseaseIds);
        this.lookup = new HpoCategoryLookup(hpo.graph(), HpoCategories.preset());
    }

    @Override
    public Set<Term> getTopLevelTerms(Collection<Term> hpoTermIds) {
        return hpoTermIds.stream()
                .flatMap(term -> lookup.getPrioritizedCategory(term.id()).stream())
                .filter(t -> hpo.containsTermId(t.id()))
                .collect(Collectors.toSet());
    }

    @Override
    public MinimalOntology ontology() {
        return hpo;
    }

    @Override
    public List<HpoDiseaseSummary> getDiseasesForGene(String entrezId) {
        return this.geneIdToDiseaseIds.getOrDefault(TermId.of(entrezId), List.of()).stream()
                .flatMap(diseaseId -> hpoDiseases.diseaseById(diseaseId).stream())
                .map(d -> HpoDiseaseSummary.of(d.id(), d.diseaseName()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<TermId> getDiseaseIdsForGene(String entrezId) {
        return this.geneIdToDiseaseIds.getOrDefault(TermId.of(entrezId), List.of());
    }

    @Override
    public List<TermId> phenotypicAbnormalitiesForDiseaseId(TermId diseaseId) {
        return hpoDiseases.diseaseById(diseaseId).stream()
                .flatMap(HpoDisease::presentAnnotationsStream)
                .map(HpoDiseaseAnnotation::id)
                .collect(Collectors.toList());
    }

}
