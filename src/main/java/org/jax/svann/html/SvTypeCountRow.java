package org.jax.svann.html;

import org.jax.svann.reference.SvType;

import java.util.Map;

/**
 * This is a convenience class that makes it easier to organize information needed to display Structural Variants
 * according to both type and impact. Each instance of this class holds the counts for one particular {@link SvType}.
 * @author Peter N Robinson
 */
public class SvTypeCountRow {

    private final String name;
    private final int low;
    private final int intermediate;
    private final int high;
    private final int total;

    public SvTypeCountRow(SvType svtype, int lo, int med, int hi) {
        this.name = svtype.name();
        this.low = lo;
        this.intermediate = med;
        this.high = hi;
        this.total = lo + med + hi;
    }

    public String getName() {
        return name;
    }

    public int getLow() {
        return low;
    }

    public int getIntermediate() {
        return intermediate;
    }

    public int getHigh() {
        return high;
    }

    public int getTotal() {
        return total;
    }

    /**
     * This constructor is used to make the very last row of the table, with the grand totals
     * @param lowImpact Counts map for all low impact structural variants
     * @param intermediateImpact Counts map for all intermediate impact structural variants
     * @param highImpact Counts map for all high impact structural variants
     */
    public  SvTypeCountRow(Map<SvType, Integer> lowImpact,
                                             Map<SvType, Integer> intermediateImpact,
                                             Map<SvType, Integer> highImpact) {
        this.low = lowImpact.values().stream().reduce(Integer::sum).orElse(0);
        this.intermediate = intermediateImpact.values().stream().reduce(Integer::sum).orElse(0);
        this.high = highImpact.values().stream().reduce(Integer::sum).orElse(0);
        this.total = this.low + this.intermediate + this.high;
        this.name = "Total";

    }


}
