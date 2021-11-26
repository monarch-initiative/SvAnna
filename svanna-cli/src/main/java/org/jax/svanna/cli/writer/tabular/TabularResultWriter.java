package org.jax.svanna.cli.writer.tabular;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jax.svanna.cli.writer.AnalysisResults;
import org.jax.svanna.cli.writer.ResultWriter;
import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.filter.FilterType;
import org.jax.svanna.core.filter.Filterable;
import org.jax.svanna.core.priority.Prioritized;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class TabularResultWriter implements ResultWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TabularResultWriter.class);

    private final String suffix;

    private final char columnSeparator;

    private final boolean compress;

    public TabularResultWriter(String suffix, char columnSeparator, boolean compress) {
        this.suffix = suffix;
        this.columnSeparator = columnSeparator;
        this.compress = compress;
    }

    @Override
    public void write(AnalysisResults analysisResults, String prefix) throws IOException {
        try (BufferedWriter writer = openWriter(prefix)) {
            CSVPrinter printer = CSVFormat.DEFAULT.withDelimiter(columnSeparator)
                    .withHeader("contig", "start", "end", "id", "vtype", "failed_filters", "tadsv")
                    .print(writer);
            analysisResults.variants().stream()
                    .filter(sv -> !Double.isNaN(sv.svPriority().getPriority()))
                    .sorted(Comparator.comparing(Prioritized::svPriority).reversed())
                    .forEachOrdered(printVariant(printer));
        }
    }

    private BufferedWriter openWriter(String prefix) throws IOException {
        String pathString = prefix + suffix + (compress ? ".gz" : "");
        Path outPath = Paths.get(pathString);
        LogUtils.logInfo(LOGGER, "Writing tabular results into {}", outPath.toAbsolutePath());
        return compress
                ? new BufferedWriter(new OutputStreamWriter(new GzipCompressorOutputStream(new FileOutputStream(outPath.toFile()))))
                : Files.newBufferedWriter(outPath);
    }

    private static Consumer<? super SvannaVariant> printVariant(CSVPrinter printer) {
        return variant -> {
            try {
                printer.print(variant.contigName());
                printer.print(variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                printer.print(variant.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                printer.print(variant.id());
                printer.print(variant.variantType());
                printer.print(failedFilters(variant));
                printer.print(variant.svPriority().getPriority());
                printer.println();
            } catch (IOException e) {
                LogUtils.logWarn(LOGGER, "Error writing out record `{}`", LogUtils.variantSummary(variant));
            }
        };
    }

    private static String failedFilters(Filterable filterable) {
        List<String> failedFilters = new LinkedList<>();
        for (FilterType filterType : FilterType.svannaFilterTypes()) {
            if (filterable.failedFilter(filterType))
                failedFilters.add(filterType.vcfValue());
        }
        return String.join(";", failedFilters);
    }
}
