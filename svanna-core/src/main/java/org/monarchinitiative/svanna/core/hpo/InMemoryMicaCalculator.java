package org.monarchinitiative.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Objects;

public class InMemoryMicaCalculator implements MicaCalculator {

    private final Map<TermPair, Double> similarityMap;

    public InMemoryMicaCalculator(Map<TermPair, Double> similarityMap) {
        this.similarityMap = Objects.requireNonNull(similarityMap, "Similarity map cannot be null");
    }

    /**
     * Return the information content of the most informative common ancestor of two HPO terms.
     *
     * @param a term
     * @param b term
     * @return information content of the most informative common ancestor of terms <code>a</code> and <code>b</code>
     */
    @Override
    public double calculate(TermId a, TermId b) {
        TermPair pair = TermPair.symmetric(a, b);
        // Note that if we do not have a value in {@link #similarityMap}, we assume the similarity is zero because
        // the MICA of the two terms is the root.
        return similarityMap.getOrDefault(pair, 0.0);
    }

}
