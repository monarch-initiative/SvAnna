package org.jax.svann.analysis;

import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.priority.SvImpact;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Filter and count structural variants according to Impact level and category.
 */
public class FilterAndCount {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterAndCount.class);

    private final Map<SvType, Integer> lowImpactCounts;
    private final Map<SvType, Integer> intermediateImpactCounts;
    private final Map<SvType, Integer> highImpactCounts;

    private final List<SvPriority> filteredPriorityList;
    /** Number of distinct gene symbols annotated as affected in any way by a structural variant. */
    private final int nAffectedGenes;
    /** Number of distinct enhancers annotated as affected in any way by a structural variant. */
    private final int nAffectedEnhancers;




    private final int unparsableCount;

    public FilterAndCount(List<SvPriority> priorityList,
                          List<? extends SequenceRearrangement> rearrangements,
                          SvImpact threshold) {
        this.lowImpactCounts = new HashMap<>();
        this.intermediateImpactCounts = new HashMap<>();
        this.highImpactCounts = new HashMap<>();
        Set<Enhancer> affectedEnhancers = new HashSet<>();
        Set<String> affectedGenes = new HashSet<>();
        int unknown = 0;
        // Initialize the count maps to be zero for all SvType
        Arrays.stream(SvType.values()).forEach(v -> {
            this.lowImpactCounts.put(v, 0);
            this.intermediateImpactCounts.put(v, 0);
            this.highImpactCounts.put(v, 0);
        });

        if (priorityList.size() != rearrangements.size()) {
            LOGGER.warn("Number of priorities and rearrangements does not match: {}!={}", priorityList.size(), rearrangements.size());
            throw new SvAnnRuntimeException("Number of priorities and rearrangements does not match");
        }

        // iterate through priorities and rearrangements
        for (int i = 0; i < priorityList.size(); i++) {
            SvPriority svPriority = priorityList.get(i);
            SequenceRearrangement rearrangement = rearrangements.get(i);

            switch (svPriority.getImpact()) {
                case HIGH:
                    this.highImpactCounts.merge(rearrangement.getType(), 1, Integer::sum);
                    break;
                case INTERMEDIATE:
                    this.intermediateImpactCounts.merge(rearrangement.getType(), 1, Integer::sum);
                    break;
                case LOW:
                    this.lowImpactCounts.merge(rearrangement.getType(), 1, Integer::sum);
                    break;
                default:
                    unknown++;
            }
            Set<String> symbols = svPriority.getOverlaps()
                    .stream()
                    .map(Overlap::getGeneSymbol)
                    .collect(Collectors.toSet());
            affectedGenes.addAll(symbols);
            affectedEnhancers.addAll(svPriority.getAffectedEnhancers());

        }

        this.unparsableCount = unknown;
        filteredPriorityList = priorityList
                .stream()
                .filter(svp -> svp.getImpact().satisfiesThreshold(threshold))
                .collect(Collectors.toList());
        this.nAffectedGenes = affectedGenes.size();
        this.nAffectedEnhancers = affectedEnhancers.size();
    }

    public FilterAndCount(List<SvPriority> priorityList, List<SequenceRearrangement> rearrangements) {
        this(priorityList, rearrangements, SvImpact.HIGH);
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

    public int getnAffectedGenes() {
        return nAffectedGenes;
    }

    public int getnAffectedEnhancers() {
        return nAffectedEnhancers;
    }
}
