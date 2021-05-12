package org.jax.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;

public class ResnikSimilarityScoreCalculator implements SimilarityScoreCalculator {

    private final TermSimilarityCalculator similarityCalculator;

    private final boolean symmetric;

    public ResnikSimilarityScoreCalculator(TermSimilarityCalculator similarityCalculator, boolean symmetric) {
        this.similarityCalculator = similarityCalculator;
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
                maxValue = Math.max(maxValue, similarityCalculator.calculate(q, t));
            }
            sum += maxValue;
        }
        return sum / query.size();
    }

}
