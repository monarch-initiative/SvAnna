package org.jax.svanna.cli.writer;

import java.util.Objects;

public class OutputOptions {

    private final String prefix;
    /**
     * Number of variants reported in HTML result. The variants are sorted by priority.
     */
    private final int nVariantsToReport;

    public OutputOptions(String prefix, int nVariantsToReport) {
        this.prefix = prefix;
        this.nVariantsToReport = nVariantsToReport;
    }

    public String prefix() {
        return prefix;
    }

    public int nVariantsToReport() {
        return nVariantsToReport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutputOptions that = (OutputOptions) o;
        return nVariantsToReport == that.nVariantsToReport && Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, nVariantsToReport);
    }

    @Override
    public String toString() {
        return "OutputOptions{" +
                "prefix='" + prefix + '\'' +
                ", nVariantsToReport=" + nVariantsToReport +
                '}';
    }
}
