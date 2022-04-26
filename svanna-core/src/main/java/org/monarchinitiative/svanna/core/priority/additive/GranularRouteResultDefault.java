package org.monarchinitiative.svanna.core.priority.additive;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

class GranularRouteResultDefault implements GranularRouteResult {

    private final Map<String, Double> scores;

    static GranularRouteResult of(Map<String, Double> scores) {
        return new GranularRouteResultDefault(scores);
    }

    private GranularRouteResultDefault(Map<String, Double> scores) {
        this.scores = scores;
    }

    @Override
    public Set<String> geneIds() {
        return scores.keySet();
    }

    @Override
    public double geneContribution(String geneId) {
        return scores.getOrDefault(geneId, 0.D);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GranularRouteResultDefault that = (GranularRouteResultDefault) o;
        return Objects.equals(scores, that.scores);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scores);
    }

    @Override
    public String toString() {
        return "GranularRouteResultDefault{" +
                "priority=" + priority() +
                '}';
    }
}
