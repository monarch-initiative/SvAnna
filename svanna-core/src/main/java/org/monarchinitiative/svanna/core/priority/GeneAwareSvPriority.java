package org.monarchinitiative.svanna.core.priority;

import java.util.Map;
import java.util.Set;

public interface GeneAwareSvPriority extends SvPriority {

    static GeneAwareSvPriority of(Map<String, Double> contributionMap) {
        return GeneAwarePriorityDefault.of(contributionMap);
    }

    Set<String> geneIds();

    double geneContribution(String geneId);

    @Override
    default double getPriority() {
        return geneIds().stream()
                .mapToDouble(this::geneContribution)
                .sum();
    }

}
