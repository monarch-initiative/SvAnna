package org.jax.svanna.cli.cmd;


import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.prioritizer.SvImpact;
import org.jax.svanna.core.prioritizer.SvPriority;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.variant.api.VariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Filter and count structural variants according to Impact level and category.
 */
public class FilterAndCount {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterAndCount.class);

    private final Map<VariantType, Integer> lowImpactCounts;
    private final Map<VariantType, Integer> intermediateImpactCounts;
    private final Map<VariantType, Integer> highImpactCounts;

    private final List<SvPriority> filteredPriorityList;
    /** Number of distinct gene symbols annotated as affected in any way by a structural variant. */
    private final int nAffectedGenes;
    /** Number of distinct enhancers annotated as affected in any way by a structural variant. */
    private final int nAffectedEnhancers;




    private final int unparsableCount;

    public FilterAndCount(List<SvPriority> priorityList,
                          List<? extends SvannaVariant> variants,
                          SvImpact threshold) {
        this.lowImpactCounts = new HashMap<>();
        this.intermediateImpactCounts = new HashMap<>();
        this.highImpactCounts = new HashMap<>();
        Set<Enhancer> affectedEnhancers = new HashSet<>();
        Set<String> affectedGenes = new HashSet<>();
        int unknown = 0;
        // Initialize the count maps to be zero for all SvType
        Arrays.stream(VariantType.values()).forEach(v -> {
            this.lowImpactCounts.put(v, 0);
            this.intermediateImpactCounts.put(v, 0);
            this.highImpactCounts.put(v, 0);
        });

        if (priorityList.size() != variants.size()) {
            LOGGER.warn("Number of priorities and variants does not match: {}!={}", priorityList.size(), variants.size());
            throw new SvAnnRuntimeException("Number of priorities and variants does not match");
        }

        // iterate through priorities and rearrangements
        for (int i = 0; i < priorityList.size(); i++) {
            SvPriority svPriority = priorityList.get(i);
            SvannaVariant variant = variants.get(i);

            switch (svPriority.getImpact()) {
                case HIGH:
                    this.highImpactCounts.merge(variant.variantType(), 1, Integer::sum);
                    break;
                case INTERMEDIATE:
                    this.intermediateImpactCounts.merge(variant.variantType(), 1, Integer::sum);
                    break;
                case LOW:
                    this.lowImpactCounts.merge(variant.variantType(), 1, Integer::sum);
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

    public FilterAndCount(List<SvPriority> priorityList, List<SvannaVariant> rearrangements) {
        this(priorityList, rearrangements, SvImpact.HIGH);
    }

    public Map<VariantType, Integer> getLowImpactCounts() {
        return lowImpactCounts;
    }

    public Map<VariantType, Integer> getIntermediateImpactCounts() {
        return intermediateImpactCounts;
    }

    public Map<VariantType, Integer> getHighImpactCounts() {
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
