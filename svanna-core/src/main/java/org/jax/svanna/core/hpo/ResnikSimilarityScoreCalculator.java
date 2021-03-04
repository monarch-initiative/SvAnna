package org.jax.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Map;

public class ResnikSimilarityScoreCalculator implements SimilarityScoreCalculator {

    private final Map<TermPair, Double> similarityMap;

    private final boolean symmetric;

    public ResnikSimilarityScoreCalculator(Map<TermPair, Double> similarityMap, boolean symmetric) {
        this.similarityMap = similarityMap;
        this.symmetric = symmetric;
    }

    @Override
    public double computeSimilarityScore(Collection<TermId> query, Collection<TermId> target) {
        return symmetric
                ? computeScoreSymmetric(query, target)
                : computeScoreAsymmetric(query, target);
    }

    public double computeScoreSymmetric(Collection<TermId> query, Collection<TermId> target) {
        return 0.5 * (computeScoreImpl(query, target) + computeScoreImpl(target, query));

    }

    public double computeScoreAsymmetric(Collection<TermId> query, Collection<TermId> target) {
        return computeScoreImpl(query, target);
    }

    /**
     * Compute directed score between a query and a target set of {@link TermId}s.
     *
     * @param query Query set of {@link TermId}s.
     * @param target Target set of {@link TermId}s
     * @return Symmetric similarity score between <code>query</code> and <code>target</code>.
     */
    private double computeScoreImpl(Collection<TermId> query, Collection<TermId> target) {
        double sum = 0;
        for (TermId q : query) {
            double maxValue = 0.0;
            for (TermId t : target) {
                maxValue = Math.max(maxValue, getResnikSymmetric(q, t));
            }
            sum += maxValue;
        }
        return sum / query.size();
    }

    /**
     * Return the Resnik similarity between two HPO terms. Note that if we do not have a
     * value in {@link #similarityMap}, we asssume the similarity is zero because
     * the MICA of the two terms is the root.
     *
     * @param a The first TermId
     * @param b The second TermId
     * @return the Resnik similarity
     */
    public double getResnikSymmetric(TermId a, TermId b) {
        TermPair tpair = TermPair.symmetric(a, b);
        return similarityMap.getOrDefault(tpair, 0.0);
    }
}
