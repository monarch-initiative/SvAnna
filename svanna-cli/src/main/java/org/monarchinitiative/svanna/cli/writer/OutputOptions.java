package org.monarchinitiative.svanna.cli.writer;

import java.nio.file.Path;
import java.util.Objects;

public class OutputOptions {

    /**
     * Path to the output folder.
     */
    private final Path output;
    private final String prefix;
    /**
     * Number of variants reported in HTML result. The variants are sorted by priority.
     */
    private final int nVariantsToReport;

    public OutputOptions(Path output,
                         String prefix,
                         int nVariantsToReport) {
        this.output = Objects.requireNonNull(output);
        this.prefix = Objects.requireNonNull(prefix);
        this.nVariantsToReport = nVariantsToReport;
    }

    public Path output() {
        return output;
    }

    public String prefix() {
        return prefix;
    }

    public int nVariantsToReport() {
        return nVariantsToReport;
    }

    @Override
    public String toString() {
        return "OutputOptions{" +
                "prefix='" + prefix + '\'' +
                ", nVariantsToReport=" + nVariantsToReport +
                '}';
    }
}
