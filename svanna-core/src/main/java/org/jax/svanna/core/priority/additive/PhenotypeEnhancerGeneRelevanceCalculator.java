package org.jax.svanna.core.priority.additive;

import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Set;

public class PhenotypeEnhancerGeneRelevanceCalculator implements EnhancerGeneRelevanceCalculator {

    private final Set<TermId> topLevelProbandTerms;

    public static PhenotypeEnhancerGeneRelevanceCalculator of(Set<TermId> topLevelProbandTerms) {
        return new PhenotypeEnhancerGeneRelevanceCalculator(topLevelProbandTerms);
    }

    private PhenotypeEnhancerGeneRelevanceCalculator(Set<TermId> topLevelProbandTerms) {
        this.topLevelProbandTerms = topLevelProbandTerms;
    }

    @Override
    public double calculateRelevance(Enhancer enhancer) {
        for (TermId hpoTermAssociation : enhancer.hpoTermAssociations()) {
            if (topLevelProbandTerms.contains(hpoTermAssociation))
                return 1.;
        }
        return 0.;
    }
}
