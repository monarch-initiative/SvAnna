package org.jax.svanna.cli.writer.html;

import java.util.Objects;

public class HtmlResultFormatParameters {

    public static int DEFAULT_N_VARIANTS_REPORTED = Integer.MAX_VALUE;
    public static int DEFAULT_MIN_ALT_READ_SUPPORT = 1;

    private final int reportNVariants;
    private final int minAltReadSupport;

    public HtmlResultFormatParameters(int reportNVariants, int minAltReadSupport) {
        this.reportNVariants = reportNVariants;
        this.minAltReadSupport = minAltReadSupport;
    }

    public int reportNVariants() {
        return reportNVariants;
    }

    public int minAltReadSupport() {
        return minAltReadSupport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HtmlResultFormatParameters that = (HtmlResultFormatParameters) o;
        return reportNVariants == that.reportNVariants && minAltReadSupport == that.minAltReadSupport;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportNVariants, minAltReadSupport);
    }

    static HtmlResultFormatParameters defaultParameters() {
        return new HtmlResultFormatParameters(DEFAULT_N_VARIANTS_REPORTED, DEFAULT_MIN_ALT_READ_SUPPORT);
    }
}
