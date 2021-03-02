package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.reference.Gene;

/**
 * Assess the relevance of a potential change to the gene for an individual with given phenotype.
 */
@FunctionalInterface
public interface GeneWeightCalculator {

    double DEFAULT_WEIGHT = 1.;

    /**
     * @return calculator that assumes that any gene has a unit relevance wrt. individual's condition
     */
    static GeneWeightCalculator defaultGeneRelevanceCalculator() {
        return gene -> DEFAULT_WEIGHT;
    }

    double calculateRelevance(Gene gene);
}
