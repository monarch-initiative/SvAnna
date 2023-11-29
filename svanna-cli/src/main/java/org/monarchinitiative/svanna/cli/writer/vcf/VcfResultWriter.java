package org.monarchinitiative.svanna.cli.writer.vcf;

import htsjdk.samtools.util.BlockCompressedOutputStream;
import htsjdk.tribble.TribbleException;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.*;
import org.monarchinitiative.svanna.cli.writer.AnalysisResults;
import org.monarchinitiative.svanna.cli.writer.OutputFormat;
import org.monarchinitiative.svanna.cli.writer.OutputOptions;
import org.monarchinitiative.svanna.cli.writer.ResultWriter;
import org.monarchinitiative.svanna.core.LogUtils;
import org.monarchinitiative.svanna.core.priority.Prioritized;
import org.monarchinitiative.svanna.core.priority.SvPriority;
import org.monarchinitiative.svanna.io.FullSvannaVariant;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

public class VcfResultWriter implements ResultWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VcfResultWriter.class);

    private static final String SVANNA_PSV_FIELD_NAME = "PSV";
    private static final VCFInfoHeaderLine PSV_LINE = new VCFInfoHeaderLine(
            SVANNA_PSV_FIELD_NAME,
            VCFHeaderLineCount.A,
            VCFHeaderLineType.Float,
            "SvAnna PSV score for the variant");

    private final boolean compress;

    public VcfResultWriter(boolean compress) {
        this.compress = compress;
    }

    /**
     * Extend the <code>header</code> with INFO fields that are being added in this command.
     *
     * @return the extended header
     */
    private static VCFHeader prepareVcfHeader(Path inputVcfPath) {
        VCFHeader header;
        try (VCFFileReader reader = new VCFFileReader(inputVcfPath, false)) {
            header = reader.getFileHeader();
        } catch (TribbleException.MalformedFeatureFile e) {
            // This happens when the input variants were not read from a VCF file but from e.g. a CSV file. In case we
            // add another variant source in future
            LOGGER.info("Creating a stub VCF header");
            header = new VCFHeader();
            header.setVCFHeaderVersion(VCFHeaderVersion.VCF4_2);
        }

        // TADSV - float
        header.addMetaDataLine(PSV_LINE);

        return header;
    }

    private static Function<FullSvannaVariant, Optional<VariantContext>> addInfoField() {
        return sv -> {
            SvPriority svPriority = sv.svPriority();

            VariantContext vc = sv.variantContext();
            if (vc == null) {
                GenomicVariant gv = sv.genomicVariant();
                LOGGER.debug("Cannot write VCF line for variant '{}' because variant context is missing. {}:{}{}>{}",
                        gv.id(), gv.contig().name(), gv.startOnStrandWithCoordinateSystem(gv.strand(), CoordinateSystem.oneBased()), gv.ref(), gv.alt());
                return Optional.empty();
            }

            if (svPriority == null || Double.isNaN(svPriority.getPriority()))
                return Optional.of(vc);

            VariantContextBuilder builder = new VariantContextBuilder(vc);
            return Optional.of(builder.attribute(SVANNA_PSV_FIELD_NAME, svPriority.getPriority())
                    .make());
        };
    }

    @Override
    public void write(AnalysisResults analysisResults, OutputOptions outputOptions) throws IOException {
        Path inputVcfPath = Paths.get(analysisResults.variantSource());
        VCFHeader header = prepareVcfHeader(inputVcfPath);

        Path outPath = outputOptions.output().resolve(outputOptions.prefix() + OutputFormat.VCF.fileSuffix() + (compress ? ".gz" : ""));
        LogUtils.logInfo(LOGGER, "Writing VCF results into {}", outPath.toAbsolutePath());

        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .setOutputVCFStream(openOutputStream(outPath))
                .setReferenceDictionary(header.getSequenceDictionary())
                .unsetOption(Options.INDEX_ON_THE_FLY)
                .build()) {
            writer.writeHeader(header);

            analysisResults.variants().stream()
                    .filter(sv -> !Double.isNaN(sv.svPriority().getPriority()))
                    .sorted(Comparator.comparing(Prioritized::svPriority).reversed())
                    .map(addInfoField())
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEachOrdered(writer::add);
        }
    }


    private BufferedOutputStream openOutputStream(Path outputPath) throws IOException {
        return compress
                ? new BufferedOutputStream(new BlockCompressedOutputStream(outputPath.toFile()))
                : new BufferedOutputStream(Files.newOutputStream(outputPath));
    }
}
