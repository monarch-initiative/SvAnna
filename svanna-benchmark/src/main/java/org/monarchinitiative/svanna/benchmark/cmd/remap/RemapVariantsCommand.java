package org.monarchinitiative.svanna.benchmark.cmd.remap;

import htsjdk.variant.vcf.VCFFileReader;
import org.monarchinitiative.svanna.benchmark.Main;
import org.monarchinitiative.svanna.benchmark.cmd.remap.lift.LiftFullSvannaVariant;
import org.monarchinitiative.svanna.benchmark.cmd.remap.write.BedWriter;
import org.monarchinitiative.svanna.benchmark.cmd.remap.write.FullSvannaVariantWriter;
import org.monarchinitiative.svanna.benchmark.cmd.remap.write.SvTool;
import org.monarchinitiative.svanna.benchmark.cmd.remap.write.VcfWriter;
import org.monarchinitiative.svanna.io.FullSvannaVariant;
import org.monarchinitiative.svanna.core.io.VariantParser;
import org.monarchinitiative.svanna.io.parse.VcfVariantParser;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.util.VariantTrimmer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommandLine.Command(name = "remap-variants",
        aliases = {"RV"},
        header = "Remap structural variants.",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class RemapVariantsCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemapVariantsCommand.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    @CommandLine.Option(names = "--genomic-assembly",
            description = "target genomic assembly [GRCh37, GRCh38] (default: ${DEFAULT-VALUE})")
    public String genomicAssemblyVersion = "GRCh38";

    @CommandLine.Option(names = "--output-format",
            description = "output format [VCF, BED], (default: ${DEFAULT-VALUE})")
    public String outputFormat = "VCF";

    @CommandLine.Option(names = "--liftover-chain",
            description = "path to Liftover chain for conversion from GRCh38 to GRCh37. Must be provided if `--genomic-assembly GRCh37`")
    public Path liftoverChainPath;

    @CommandLine.Option(names = "--bed-output-format",
            type = SvTool.class,
            description = "format BED output for the tool ([X_CNV, CLASSIFY_CNV])")
    public SvTool tool;

    @CommandLine.Parameters(
            index = "0",
            description = "path to VCF file with structural variants")
    public Path inputVcfPath;

    @CommandLine.Parameters(
            index = "1",
            description = "where to store the output file")
    public Path outputPath;

    private LiftFullSvannaVariant lift;

    private static VariantTrimmer.BaseRetentionStrategy prepareRetentionStrategy(String outputFormat) {
        if ("BED".equals(outputFormat)) {
            return VariantTrimmer.removingCommonBase();
        } else if ("VCF".equals(outputFormat)) {
            // we need that leading base here!
            return VariantTrimmer.retainingCommonBase();
        } else {
            throw new RuntimeException("Invalid output format: " + outputFormat);
        }
    }

    private static String readSampleName(Path inputVcfPath) {
        try (VCFFileReader reader = new VCFFileReader(inputVcfPath, false)) {
            return reader.getFileHeader().getSampleNamesInOrder().get(0);
        }
    }

    @Override
    public Integer call() throws Exception {
        // 1. read input variants
        LOGGER.info("Reading variants from {}", inputVcfPath.toAbsolutePath());
        VariantTrimmer.BaseRetentionStrategy retentionStrategy = prepareRetentionStrategy(outputFormat);
        VariantParser<? extends FullSvannaVariant> parser = new VcfVariantParser(GenomicAssemblies.GRCh38p13(), retentionStrategy);
        List<? extends FullSvannaVariant> variants = parser.createVariantAlleleList(inputVcfPath);
        String sampleName = readSampleName(inputVcfPath);

        LOGGER.info("Read {} variants", NF.format(variants.size()));

        // 2. remap if necessary
        boolean convertToGrch37 = "GRCh37".equals(genomicAssemblyVersion);
        if (convertToGrch37) {
            LOGGER.info("Remapping variants to GRCh37");
            lift = new LiftFullSvannaVariant(GenomicAssemblies.GRCh37p13(), liftoverChainPath);
        }

        List<FullSvannaVariant> remappedVariants = variants.stream()
                .map(convertCoordinates(convertToGrch37, lift))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        LOGGER.info("Remapped {} variants", NF.format(remappedVariants.size()));

        // 3. format to VCF or BED & write
        return writeVariants(sampleName, remappedVariants);
    }

    private int writeVariants(String sampleName, List<FullSvannaVariant> remappedVariants) {
        boolean convertToBed = "BED".equals(outputFormat);
        LOGGER.info("Storing variants in {} format into {} ", outputPath.toAbsolutePath(), outputFormat);
        FullSvannaVariantWriter writer;
        if (convertToBed) {
            if (tool == null) {
                LOGGER.error("`--bed-output-format` option must be set when writing output in BED format");
                return 1;
            }
            writer = new BedWriter(outputPath, tool);
        } else {
            writer = new VcfWriter(sampleName, outputPath);
        }

        int writtenLines = writer.write(remappedVariants);
        LOGGER.info("Stored {} variants", NF.format(writtenLines));
        return 0;
    }

    private static Function<? super FullSvannaVariant, Optional<? extends FullSvannaVariant>> convertCoordinates(boolean convertToGrch37,
                                                                                                                LiftFullSvannaVariant lift) {
        return variant -> {
            if (!convertToGrch37) {
                // no-op
                return Optional.of(variant);
            } else {
                return lift.lift(variant);
            }
        };
    }
}
