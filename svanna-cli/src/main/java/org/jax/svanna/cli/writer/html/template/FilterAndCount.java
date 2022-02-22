package org.jax.svanna.cli.writer.html.template;


import org.jax.svanna.cli.writer.html.VariantLandscape;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.svart.VariantType;
import org.monarchinitiative.sgenes.model.Gene;

import java.util.*;
import java.util.stream.Collectors;

import static org.jax.svanna.cli.writer.html.template.ImpactFilterCategory.*;

/**
 * Filter and count structural variants according to Impact level and category.
 */
public class FilterAndCount {

    private final Map<ImpactFilterCategory, Map<VariantType, Integer>> categoryToByVariantTypeCountMap;

    /**
     * Number of distinct gene symbols annotated as affected in any way by a structural variant.
     */
    private final int nAffectedGenes;
    /**
     * Number of distinct enhancers annotated as affected in any way by a structural variant.
     */
    private final int nAffectedEnhancers;

    private final int unableToBePrioritized;


    public FilterAndCount(List<VariantLandscape> variantLandscapes, int minAltAllele) {
        this.categoryToByVariantTypeCountMap = new HashMap<>();
        for (var cat : ImpactFilterCategory.values()) {
            this.categoryToByVariantTypeCountMap.put(cat, new HashMap<>());
            // Initialize the count maps to be zero for all SvTypes
            var countMap = categoryToByVariantTypeCountMap.get(cat);
            Arrays.stream(VariantType.values()).forEach(v -> countMap.put(v, 0));
        }
        Set<Enhancer> affectedEnhancers = new HashSet<>();
        Set<String> affectedGenes = new HashSet<>();
        int unknown = 0;

        // iterate through priorities and rearrangements
        for (VariantLandscape variantLandscape : variantLandscapes) {
            SvannaVariant variant = variantLandscape.variant();
            VariantType vt = variant.genomicVariant().variantType();
            if (variant.numberOfAltReads() < minAltAllele) {
                this.categoryToByVariantTypeCountMap.get(ALT_ALLELE_COUNT).merge(vt, 1, Integer::sum);
            } else if (!variant.passedFilters()) {
                this.categoryToByVariantTypeCountMap.get(FILTERED).merge(vt, 1, Integer::sum);
            } else {
                this.categoryToByVariantTypeCountMap.get(PASS).merge(vt, 1, Integer::sum);
                double priority = variant.svPriority().getPriority();
                if (Double.isNaN(priority))
                    unknown++;
            }
            Set<String> symbols = variantLandscape.genes().stream()
                    .map(Gene::symbol)
                    .collect(Collectors.toSet());
            affectedGenes.addAll(symbols);
            affectedEnhancers.addAll(variantLandscape.enhancers());
        }
        unableToBePrioritized = unknown;
        nAffectedGenes = affectedGenes.size();
        nAffectedEnhancers = affectedEnhancers.size();
    }


    public int getUnparsableCount() {
        return unableToBePrioritized;
    }

    public int getnAffectedGenes() {
        return nAffectedGenes;
    }

    public int getnAffectedEnhancers() {
        return nAffectedEnhancers;
    }

    public String toHtmlTable() {
        StringBuilder sb = new StringBuilder();
        sb.append(filterAndCountHeader());
        Map<ImpactFilterCategory, Integer> categoryToCountMap = new HashMap<>();
        for (var ifc : ImpactFilterCategory.values()) {
            categoryToCountMap.put(ifc, 0);
        }

        Map<VariantType, Integer> variantTypeTotals = new HashMap<>();
        for (VariantType vt : VariantType.values()) {
            int rowTotal = getRowTotal(vt);
            if (rowTotal == 0) {
                variantTypeTotals.put(vt, 0);
            } else {
                sb.append("<tr><td>").append(vt.toString()).append("</td>");
                int totalForVariantType = 0;
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
        sb.append("<tfoot><tr><td>Total</td>");
        for (var cat : ImpactFilterCategory.values()) {
            sb.append("<td>").append(categoryToCountMap.get(cat)).append("</td>");
        }
        int total = categoryToCountMap.values().stream().mapToInt(Integer::intValue).sum();
        sb.append("<td>").append(total).append("</td>");
        sb.append("</tr></tfoot>");
        sb.append("</table>\n");

        List<String> zeroCountTypes = variantTypeTotals.entrySet()
                .stream()
                .filter(e -> e.getValue() == 0)
                .map(e -> e.getKey().name())
                .sorted()
                .collect(Collectors.toList());
        sb.append("<p>The following variant types had no counts: ").append(String.join(", ", zeroCountTypes)).append("</p>\n");
        return sb.toString();
    }

    private int getRowTotal(VariantType vt) {
        int total = 0;
        for (ImpactFilterCategory ifc: ImpactFilterCategory.values()) {
            int count = categoryToByVariantTypeCountMap.get(ifc).getOrDefault(vt, 0);
            total += count;
        }
        return total;
    }

    private String filterAndCountHeader() {
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
}
