package org.jax.svanna.cli.html;

import org.monarchinitiative.variant.api.VariantType;

import java.util.Map;

/**
 * This is a convenience class that makes it easier to organize information needed to display Structural Variants
 * according to both type and impact. Each instance of this class holds the counts for one particular {@link VariantType}.
 * @author Peter N Robinson
 */
public class SvTypeCountRow {

    private final String name;
    Map<ImpactFilterCategory, Integer> countmap;
    private final int total;

    public SvTypeCountRow(VariantType svtype, Map<ImpactFilterCategory, Integer> map) {
        this.name = svtype.name();
        this.countmap = map;
        this.total = map.values().stream().mapToInt(Integer::intValue).sum();
    }

    public String getName() {
        return name;
    }



    /**
     * This constructor is used to make the very last row of the table, with the grand totals
     */
    public  SvTypeCountRow(Map<ImpactFilterCategory, Integer> map) {

        this.countmap = map;
        this.total = map.values().stream().mapToInt(Integer::intValue).sum();
        this.name = "Total";

    }


}
