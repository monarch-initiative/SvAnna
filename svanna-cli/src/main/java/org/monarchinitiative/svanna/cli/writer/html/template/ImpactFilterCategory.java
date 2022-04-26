package org.monarchinitiative.svanna.cli.writer.html.template;

/**
 * These are the categories for variants that will make up the columns of the
 * HTML table we will display. We show the counts for each of these categories
 * as compared to variant type (e.g., DEL, INS, CNV, ...).
 */
public enum ImpactFilterCategory {

    PASS("Pass"),
    FILTERED("Filters"),
    ALT_ALLELE_COUNT("Low alt allele count"),
    UNABLE_TO_PRIORITIZE("Unable to prioritize");

    private final String name;
    ImpactFilterCategory(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
