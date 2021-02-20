package org.jax.svanna.cli.html;


import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.priority.DiscreteSvPriority;
import org.jax.svanna.core.priority.SvImpact;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.VariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.jax.svanna.cli.html.ImpactFilterCategory.*;

/**
 * Filter and count structural variants according to Impact level and category.
 */
public class FilterAndCount {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterAndCount.class);

    private final Map<ImpactFilterCategory, Map<VariantType, Integer>> categoryToByVariantTypeCountMap;

    // TODO - figure out how to provide these numbers without having to keeep AnnotatedSvPriority in memory
    /** Number of distinct gene symbols annotated as affected in any way by a structural variant. */
    private final int nAffectedGenes = -1;
    /** Number of distinct enhancers annotated as affected in any way by a structural variant. */
    private final int nAffectedEnhancers = -1;

    private final int unparsableCount = -1;

    private final int minAltAlleleCount;


    public FilterAndCount(List<? extends SvPriority> priorityList,
                          List<? extends SvannaVariant> variants,
                          SvImpact threshold,
                          int minAltAllele) {
        this.categoryToByVariantTypeCountMap = new HashMap<>();
        this.minAltAlleleCount = minAltAllele;
        for (var cat : ImpactFilterCategory.values()) {
            this.categoryToByVariantTypeCountMap.put(cat, new HashMap<>());
            // Initialize the count maps to be zero for all SvTypes
            var countmap = categoryToByVariantTypeCountMap.get(cat);
            Arrays.stream(VariantType.values()).forEach(v -> countmap.put(v, 0));
        }
        Set<Enhancer> affectedEnhancers = new HashSet<>();
        Set<String> affectedGenes = new HashSet<>();
        int unknown = 0;

        if (priorityList.size() != variants.size()) {
            LOGGER.warn("Number of priorities and variants does not match: {}!={}", priorityList.size(), variants.size());
            throw new SvAnnRuntimeException("Number of priorities and variants does not match");
        }

        // iterate through priorities and rearrangements
        for (int i = 0; i < priorityList.size(); i++) {
            SvPriority svPriority = priorityList.get(i);
            SvannaVariant variant = variants.get(i);
            if (variant.numberOfAltReads() < 2) {
                this.categoryToByVariantTypeCountMap.get(ALT_ALLELE_COUNT).merge(variant.variantType(), 1, Integer::sum);
            } else if (!variant.passedFilters()) {
                this.categoryToByVariantTypeCountMap.get(FILTERED).merge(variant.variantType(), 1, Integer::sum);
            } else {
                double priority = svPriority.getPriority();
                if (priority >= .8) {
                    this.categoryToByVariantTypeCountMap.get(VERY_HIGH_IMPACT).merge(variant.variantType(), 1, Integer::sum);
                } else if (priority >= .6) {
                    this.categoryToByVariantTypeCountMap.get(HIGH_IMPACT).merge(variant.variantType(), 1, Integer::sum);
                } else if (priority >= .4) {
                    this.categoryToByVariantTypeCountMap.get(INTERMEDIATE_IMPACT).merge(variant.variantType(), 1, Integer::sum);
                } else if (priority >= .2) {
                    this.categoryToByVariantTypeCountMap.get(LOW_IMPACT).merge(variant.variantType(), 1, Integer::sum);
                } else {
                    unknown++;
                }
            }
        }
    }

    public FilterAndCount(List<? extends DiscreteSvPriority> priorityList, List<? extends SvannaVariant> variants, int minAltAllele) {
        this(priorityList, variants, SvImpact.HIGH, minAltAllele);
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

    private String header() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"counts\">\n");
        sb.append("<caption>Variant counts</caption>");
        sb.append("  <thead><tr>");
        sb.append("<th>Variant category</th>");
        for (var cat : ImpactFilterCategory.values()) {
            // the enum is listed in the sort order we want!
            sb.append("<th>").append(cat.toString()).append("</th>");
        }
        sb.append("<th>Total</th>");
        sb.append("</tr></thead>\n");
        return sb.toString();
    }

    int getRowTotal(VariantType vt) {
        int total = 0;
        for (ImpactFilterCategory ifc: ImpactFilterCategory.values()) {
            int count = categoryToByVariantTypeCountMap.get(ifc).getOrDefault(vt, 0);
            total += count;
        }
        return total;
    }

    public String toHtmlTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(header());
        Map<VariantType, Integer> variantTypeTotals = new HashMap<>();
        Map<ImpactFilterCategory, Integer> categoryToCountMap = new HashMap<>();
        for (var ifc : ImpactFilterCategory.values()) { categoryToCountMap.put(ifc, 0); }
        for (VariantType vt : VariantType.values()) {
            int totalForVariantType = getRowTotal(vt);
            if (totalForVariantType == 0) {
                variantTypeTotals.put(vt, 0);
            } else {
                sb.append("<tr><td>").append(vt.toString()).append("</td>");
                for (ImpactFilterCategory cat : ImpactFilterCategory.values()) {
                    int count = categoryToByVariantTypeCountMap.get(cat).getOrDefault(vt, 0);
                    categoryToCountMap.merge(cat, count, Integer::sum);
                    totalForVariantType += count;
                    sb.append("<td>").append(count).append("</td>");
                }
                sb.append("<td>").append(totalForVariantType).append("</td>\n");
                variantTypeTotals.put(vt, totalForVariantType);
            }
        }
        List<String> zeroCountTypes = variantTypeTotals.entrySet()
                .stream()
                .filter(e -> e.getValue() == 0)
                .map(e -> e.getKey().name())
                .sorted()
                .collect(Collectors.toList());
        sb.append("<tfoot><tr><td>Total</td>");
        for (var cat : ImpactFilterCategory.values()) {
            sb.append("<td>").append(categoryToCountMap.get(cat)).append("</td>");
        }
        int total = categoryToCountMap.values().stream().mapToInt(Integer::intValue).sum();
        sb.append("<td>").append(total).append("</td>");
        sb.append("</tr></tfoot>");
        sb.append("</table>\n");
        sb.append("<p>The following variant types had no counts: ").append(String.join(", ", zeroCountTypes)).append("</p>\n");
       return sb.toString();
    }
}
