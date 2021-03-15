package org.jax.svanna.cli.writer.html;

import org.jax.svanna.core.priority.SvImpact;

import java.util.Objects;

public class HtmlResultFormatParameters {

    public static SvImpact DEFAULT_SV_IMPACT = SvImpact.HIGH;
    public static int DEFAULT_N_VARIANTS_REPORTED = Integer.MAX_VALUE;
    public static int DEFAULT_MIN_ALT_READ_SUPPORT = 1;

    private final SvImpact threshold;
    private final int reportNVariants;
    private final int minAltReadSupport;

    public HtmlResultFormatParameters(SvImpact threshold, int reportNVariants, int minAltReadSupport) {
        this.threshold = threshold;
        this.reportNVariants = reportNVariants;
        this.minAltReadSupport = minAltReadSupport;
    }

    public SvImpact threshold() {
        return threshold;
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
        return reportNVariants == that.reportNVariants && minAltReadSupport == that.minAltReadSupport && threshold == that.threshold;
    }

    @Override
    public int hashCode() {
        return Objects.hash(threshold, reportNVariants, minAltReadSupport);
    }

    static HtmlResultFormatParameters defaultParameters() {
        return new HtmlResultFormatParameters(DEFAULT_SV_IMPACT, DEFAULT_N_VARIANTS_REPORTED, DEFAULT_MIN_ALT_READ_SUPPORT);
    }
}
