package org.monarchinitiative.svanna.cli.writer.tabular;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.svanna.cli.writer.AnalysisResults;
import org.monarchinitiative.svanna.cli.writer.OutputOptions;
import org.monarchinitiative.svanna.cli.writer.ResultWriter;
import org.monarchinitiative.svanna.core.LogUtils;
import org.monarchinitiative.svanna.core.filter.FilterType;
import org.monarchinitiative.svanna.core.filter.Filterable;
import org.monarchinitiative.svanna.core.priority.Prioritized;
import org.monarchinitiative.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

public class TabularResultWriter implements ResultWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TabularResultWriter.class);

    private static final String[] HEADER = new String[]{"contig", "start", "end", "id", "vtype", "failed_filters", "psv"};

    private final String suffix;

    private final char columnSeparator;

    private final boolean compress;

    public TabularResultWriter(String suffix, char columnSeparator, boolean compress) {
        this.suffix = suffix;
        this.columnSeparator = columnSeparator;
        this.compress = compress;
    }

    @Override
    public void write(AnalysisResults analysisResults, OutputOptions outputOptions) throws IOException {
        try (BufferedWriter writer = openWriter(outputOptions.output(), outputOptions.prefix())) {
            CSVPrinter printer = CSVFormat.DEFAULT.withDelimiter(columnSeparator)
                    .withHeader(HEADER)
                    .print(writer);
            analysisResults.variants().stream()
                    .filter(sv -> !Double.isNaN(sv.svPriority().getPriority()))
                    .sorted(Comparator.comparing(Prioritized::svPriority).reversed())
                    .forEachOrdered(printVariant(printer));
        }
    }

    private BufferedWriter openWriter(Path output, String prefix) throws IOException {
        Path outPath = output.resolve(prefix + suffix + (compress ? ".gz" : ""));
        LogUtils.logInfo(LOGGER, "Writing tabular results into {}", outPath.toAbsolutePath());
        return compress
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outPath.toFile()))))
                : Files.newBufferedWriter(outPath);
    }

    private static Consumer<? super SvannaVariant> printVariant(CSVPrinter printer) {
        return variant -> {
            GenomicVariant gv = variant.genomicVariant();
            try {
                printer.print(gv.contig().name());
                printer.print(gv.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                printer.print(gv.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                printer.print(gv.id());
                printer.print(gv.variantType());
                printer.print(failedFilters(variant));
                printer.print(variant.svPriority().getPriority());
                printer.println();
            } catch (IOException e) {
                LogUtils.logWarn(LOGGER, "Error writing out record `{}`", LogUtils.variantSummary(gv));
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
