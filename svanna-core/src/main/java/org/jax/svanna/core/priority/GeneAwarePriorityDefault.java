package org.jax.svanna.core.priority;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class GeneAwarePriorityDefault implements GeneAwareSvPriority {

    private final Map<String, Double> contributionMap;

    static GeneAwarePriorityDefault of(Map<String, Double> contributionMap) {
        return new GeneAwarePriorityDefault(contributionMap);
    }

    private GeneAwarePriorityDefault(Map<String, Double> contributionMap) {
        this.contributionMap = contributionMap;
    }

    @Override
    public Set<String> geneIds() {
        return contributionMap.keySet();
    }

    @Override
    public double geneContribution(String geneId) {
        return contributionMap.getOrDefault(geneId, 0.D);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneAwarePriorityDefault that = (GeneAwarePriorityDefault) o;
        return Objects.equals(contributionMap, that.contributionMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contributionMap);
    }

    @Override
    public String toString() {
        return "GeneAwarePriority{" +
                "priority=" + getPriority() + ',' +
                "contributingGenes=" + contributionMap.entrySet().stream()
                .filter(e -> e.getValue() > 1E-9)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(", ", "'", "'")) +
                '}';
    }
}
