package org.jax.svanna.io.hpo;

import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.priority.additive.GeneWeightCalculator;
import org.jax.svanna.core.reference.Gene;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.HpoResnikSimilarity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ResnikSimilarityGeneWeightCalculator implements GeneWeightCalculator {

    private final PhenotypeDataService phenotypeDataService;

    private final HpoResnikSimilarity hpoResnikSimilarity;

    private final List<TermId> patientFeatures;

    private final Map<TermId, Collection<TermId>> diseaseIdToTermIds;

    public ResnikSimilarityGeneWeightCalculator(PhenotypeDataService phenotypeDataService,
                                                HpoResnikSimilarity hpoResnikSimilarity,
                                                List<TermId> patientTerms,
                                                Map<TermId, Collection<TermId>> diseaseIdToTermIds) {
        this.phenotypeDataService = phenotypeDataService;
        this.hpoResnikSimilarity = hpoResnikSimilarity;
        this.patientFeatures = patientTerms;
        this.diseaseIdToTermIds = diseaseIdToTermIds;
    }


    @Override
    public double calculateRelevance(Gene gene) {
        // gene -> associated diseases -> disease IDs -> max Resnik similarity
        Set<HpoDiseaseSummary> associatedDiseases = phenotypeDataService.getDiseasesForGene(gene.accessionId());

        Set<TermId> diseaseIds = associatedDiseases.stream()
                .map(HpoDiseaseSummary::getDiseaseId)
                .map(TermId::of)
                .collect(Collectors.toSet());

        double maxSimilarity = 0.;
        for (TermId diseaseId : diseaseIds) {
            Collection<TermId> diseaseHpoIds = diseaseIdToTermIds.get(diseaseId);
            double resnikSimilarity = hpoResnikSimilarity.computeScoreSymmetric(patientFeatures, diseaseHpoIds);
            maxSimilarity = Math.max(maxSimilarity, resnikSimilarity);
        }

        return maxSimilarity;
    }
}
