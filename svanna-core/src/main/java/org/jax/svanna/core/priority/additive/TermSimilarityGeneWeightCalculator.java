package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.model.HpoDiseaseSummary;
import org.jax.svanna.model.gene.Gene;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TermSimilarityGeneWeightCalculator implements GeneWeightCalculator {

    private final PhenotypeDataService phenotypeDataService;

    private final Collection<TermId> patientFeatures;

    private final Map<TermId, Collection<TermId>> diseaseIdToTermIds;

    public TermSimilarityGeneWeightCalculator(PhenotypeDataService phenotypeDataService,
                                              Collection<TermId> patientTerms,
                                              Map<TermId, Collection<TermId>> diseaseIdToTermIds) {
        this.phenotypeDataService = phenotypeDataService;
        this.patientFeatures = patientTerms;
        this.diseaseIdToTermIds = diseaseIdToTermIds;
    }


    @Override
    public double calculateRelevance(Gene gene) {
        // gene -> associated diseases -> disease IDs -> max similarity
        Set<HpoDiseaseSummary> associatedDiseases = phenotypeDataService.getDiseasesForGene(gene.accessionId());

        Set<TermId> diseaseIds = associatedDiseases.stream()
                .map(HpoDiseaseSummary::getDiseaseId)
                .map(TermId::of)
                .collect(Collectors.toSet());

        double maxSimilarity = 0.;
        for (TermId diseaseId : diseaseIds) {
            Collection<TermId> diseaseHpoIds = diseaseIdToTermIds.get(diseaseId);
            double resnikSimilarity = phenotypeDataService.computeSimilarityScore(patientFeatures, diseaseHpoIds);
            maxSimilarity = Math.max(maxSimilarity, resnikSimilarity);
        }

        return maxSimilarity;
    }
}
