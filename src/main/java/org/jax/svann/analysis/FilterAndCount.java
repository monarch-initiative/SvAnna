package org.jax.svann.analysis;

import org.jax.svann.priority.SvImpact;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.SvType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filter and count structural variants according to Impact level and category.
 */
public class FilterAndCount {

    private final SvImpact thresholdImpact;

    private final Map<SvType, Integer> lowImpactCounts;
    private final Map<SvType, Integer> intermediateImpactCounts;
    private final Map<SvType, Integer> highImpactCounts;

    private final List<SvPriority> filteredPriorityList;

    private final int unparsableCount;

    public FilterAndCount(List<SvPriority> priorityList, SvImpact threshold) {
        this.thresholdImpact = threshold;
        this.lowImpactCounts = new HashMap<>();
        this.intermediateImpactCounts = new HashMap<>();
        this.highImpactCounts = new HashMap<>();
        int unknown = 0;
        // Initialize the count maps to be zero for all SvType
        Arrays.stream(SvType.values()).forEach(v -> {
            this.lowImpactCounts.put(v, 0);
            this.intermediateImpactCounts.put(v, 0);
            this.highImpactCounts.put(v, 0);
        });
        for (var prio : priorityList) {
            switch (prio.getImpact()) {
                case HIGH_IMPACT:
                    this.highImpactCounts.merge(prio.getType(), 1, Integer::sum);
                    break;
                case INTERMEDIATE_IMPACT:
                    this.intermediateImpactCounts.merge(prio.getType(), 1, Integer::sum);
                    break;
                case LOW_IMPACT:
                    this.lowImpactCounts.merge(prio.getType(), 1, Integer::sum);
                    break;
                default:
                    unknown++;
            }
        }
        this.unparsableCount = unknown;
        filteredPriorityList = priorityList
                .stream()
                .filter(svp -> svp.getImpact().satisfiesThreshold(threshold))
                .collect(Collectors.toList());
    }

    public FilterAndCount(List<SvPriority> priorityList) {
        this(priorityList, SvImpact.HIGH_IMPACT);
    }

    public Map<SvType, Integer> getLowImpactCounts() {
        return lowImpactCounts;
    }

    public Map<SvType, Integer> getIntermediateImpactCounts() {
        return intermediateImpactCounts;
    }

    public Map<SvType, Integer> getHighImpactCounts() {
        return highImpactCounts;
    }

    public List<SvPriority> getFilteredPriorityList() {
        return filteredPriorityList;
    }

    public int getUnparsableCount() {
        return unparsableCount;
    }
}
