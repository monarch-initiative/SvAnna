package org.jax.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;

public class InMemoryTermSimilarityCalculator implements TermSimilarityCalculator {

    private final Map<TermPair, Double> similarityMap;

    public InMemoryTermSimilarityCalculator(Map<TermPair, Double> similarityMap) {
        this.similarityMap = similarityMap;
    }

    /**
     * Return the Resnik similarity between two HPO terms. Note that if we do not have a
     * value in {@link #similarityMap}, we assume the similarity is zero because
     * the MICA of the two terms is the root.
     *
     * @param a The first TermId
     * @param b The second TermId
     * @return the Resnik similarity
     */
    @Override
    public double calculate(TermId a, TermId b) {
        TermPair pair = TermPair.symmetric(a, b);
        return similarityMap.getOrDefault(pair, 0.0);
    }

}
