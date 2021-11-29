package org.jax.svanna.core.priority.additive;

import org.jax.svanna.model.landscape.enhancer.Enhancer;

/**
 * Assess the relevance of a potential change to the enhancer - gene interaction for an individual with given phenotype.
 * <p>
 * Disruption of a liver-specific enhancer is unlikely to be causal to an individual with e.g. autism.
 */
@FunctionalInterface
public interface EnhancerGeneRelevanceCalculator {

    double DEFAULT_ENHANCER_RELEVANCE = .1f;

    static EnhancerGeneRelevanceCalculator defaultCalculator() {
        return e -> DEFAULT_ENHANCER_RELEVANCE;
    }

    double calculateRelevance(Enhancer enhancer);
}
