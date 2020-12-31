package org.jax.svanna.cli.html;

/**
 * These are the categories for variants that will make up the columns of the
 * HTML table we will display. We show the counts for each of these categories
 * as compared to variant type (e.g., DEL, INS, CNV, ...).
 */
public enum ImpactFilterCategory {

    LOW_IMPACT("Low"),
    INTERMEDIATE_IMPACT("Intermediate"),
    HIGH_IMPACT("High"),
    VERY_HIGH_IMPACT("Very high"),

    FILTERED("Filters"),
    ALT_ALLELE_COUNT("Low alt allele count");

    private final String name;
    ImpactFilterCategory(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
