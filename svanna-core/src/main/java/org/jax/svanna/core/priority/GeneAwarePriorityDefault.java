package org.jax.svanna.core.priority;

import java.util.Map;
import java.util.Set;

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
}
