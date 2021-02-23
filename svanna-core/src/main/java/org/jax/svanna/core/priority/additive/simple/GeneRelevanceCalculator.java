package org.jax.svanna.core.priority.additive.simple;

import org.jax.svanna.core.reference.Gene;

/**
 * Assess the relevance of a potential change to the gene for an individual with given phenotype.
 */
@FunctionalInterface
public interface GeneRelevanceCalculator {

    double DEFAULT_RELEVANCE = 1.;

    /**
     * @return calculator that assumes that any gene has a unit relevance wrt. individual's condition
     */
    static GeneRelevanceCalculator defaultGeneRelevanceCalculator() {
        return gene -> DEFAULT_RELEVANCE;
    }

    double calculateRelevance(Gene gene);
}
