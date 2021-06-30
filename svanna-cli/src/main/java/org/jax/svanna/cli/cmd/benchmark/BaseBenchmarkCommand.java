package org.jax.svanna.cli.cmd.benchmark;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jax.svanna.cli.cmd.SvAnnaCommand;
import org.jax.svanna.core.exception.LogUtils;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

abstract class BaseBenchmarkCommand extends SvAnnaCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBenchmarkCommand.class);

    protected static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    @CommandLine.Option(names = {"--similarity-threshold"}, description = "percentage threshold for determining variant's region is similar enough to database entry (default: ${DEFAULT-VALUE})")
    public float similarityThreshold = 80.F;

    @CommandLine.Option(names = {"--frequency-threshold"}, description = "frequency threshold as a percentage [0-100] (default: ${DEFAULT-VALUE})")
    public float frequencyThreshold = 1.F;

    @CommandLine.Option(names={"--min-read-support"}, description="Minimum number of ALT reads to prioritize (default: ${DEFAULT-VALUE})")
    public int minAltReadSupport = 3;

    @CommandLine.Option(names = {"--max-length"}, description = "Do not prioritize variants longer than this (default: ${DEFAULT-VALUE})")
    public int maxLength = 100_000;

    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE})")
    public String outPrefix = "SVANNA_BB";

    @CommandLine.Option(names = {"--n-threads"}, paramLabel = "2", description = "Process variants using n threads (default: ${DEFAULT-VALUE})")
    public int nThreads = 2;

    @CommandLine.Option(names = {"--report-all-variants"}, description = "report rank and priority of all variants (default: ${DEFAULT-VALUE})")
    public boolean reportAllVariants = false;


    /**
     * Write a CSV file that represent results of one simulation.
     *
     */
    protected void writeOutResults(File output, BenchmarkResults results, Set<String> causalVariantIds) throws IOException {
        LogUtils.logDebug(LOGGER, "Ranking variants");
        List<VariantPriority> prioritized = results.priorities().stream()
                .filter(vp -> causalVariantIds.contains(vp.variant().id()) || reportAllVariants)
                .sorted(Comparator.<VariantPriority>comparingDouble(p -> p.priority().getPriority()).reversed())
                .collect(Collectors.toUnmodifiableList());

        // "case_name", "background_vcf", "variant_id", "rank", "vtype", "is_causal", "priority"
        LogUtils.logInfo(LOGGER, "Writing the results for `{}`", results.caseName());
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GzipCompressorOutputStream(new FileOutputStream(output))))) {
            CSVPrinter printer = CSVFormat.DEFAULT
                    .withHeader("case_name", "background_vcf", "variant_id", "rank", "vtype", "is_causal", "priority")
                    .print(writer);

            int rank = 1;
            for (VariantPriority priority : prioritized) {
                Variant variant = priority.variant();
                printer.print(results.caseName());
                printer.print(results.backgroundVcfName());
                printer.print(variant.id());
                printer.print(rank);
                printer.print(variant.variantType());
                printer.print(causalVariantIds.contains(variant.id()));
                printer.print(priority.priority().getPriority());
                printer.println();

                rank++;
            }
        }
    }
}
