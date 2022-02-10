package org.jax.svanna.benchmark.cmd.lift_coordinates;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jax.svanna.benchmark.Main;
import org.jax.svanna.benchmark.cmd.remap.lift.LiftFullSvannaVariant;
import org.jax.svanna.benchmark.io.CaseReport;
import org.jax.svanna.benchmark.io.CaseReportImporter;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicBreakendVariant;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "lift-coordinates",
        aliases = {"LC"},
        header = "Lift variant coordinates",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class LiftCoordinatesCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiftCoordinatesCommand.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    @CommandLine.Option(names = "--genomic-assembly",
            description = "target genomic assembly [GRCh37, GRCh38] (default: ${DEFAULT-VALUE})")
    public String genomicAssemblyVersion = "GRCh38";

    @CommandLine.Option(names = "--liftover-chain",
            description = "path to Liftover chain for conversion from GRCh38 to GRCh37. Must be provided if `--genomic-assembly GRCh37`")
    public Path liftoverChainPath;

    @CommandLine.Parameters(
            index = "0",
            description = "path to VCF file with structural variants")
    public Path caseFolderPath;

    @CommandLine.Parameters(
            index = "1",
            description = "where to store the output file")
    public Path outputPath;

    private LiftFullSvannaVariant lift;

    @Override
    public Integer call() throws Exception {
        // 1. Read all cases of a folder
        List<CaseReport> cases = CaseReportImporter.readCasesProvidedViaCaseFolderOption(caseFolderPath);

        LOGGER.info("Read {} cases", NF.format(cases.size()));

        // 2. Lift if necessary and write
        String[] header = {
                "case_id", "variant_id",
                "grch38_contig", "grch38_start", "grch38_end",
                "grch37_contig", "grch37_start", "grch37_end"
        };
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath);
             CSVPrinter printer = CSVFormat.DEFAULT.withHeader(header).print(writer)) {

            boolean liftToGrch37 = "GRCh37".equals(genomicAssemblyVersion);
            if (liftToGrch37) {
                LOGGER.info("Remapping variants to GRCh37");
                lift = new LiftFullSvannaVariant(GenomicAssemblies.GRCh37p13(), liftoverChainPath);
            }
            for (CaseReport caseReport : cases) {

                for (SvannaVariant variant : caseReport.variants()) {
                    Optional<? extends GenomicVariant> liftedOptional = liftIfNecessary(liftToGrch37, variant);

                    if (liftedOptional.isPresent()) {
                        printer.print(caseReport.caseSummary().phenopacketId());
                        printer.print(variant.id());
                        // GRCh38
                        printer.print(variant.contigName());
                        printer.print(variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                        printer.print(variant.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                        // GRCh37
                        GenomicVariant lv = liftedOptional.get();
                        printer.print(lv.contigName());
                        printer.print(lv.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                        printer.print(lv.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
                        printer.println();
                    }
                }
            }
        }

        LOGGER.info("Write output to {}", outputPath.toAbsolutePath());
        return 0;
    }

    private Optional<? extends GenomicVariant> liftIfNecessary(boolean liftToGrch37, SvannaVariant variant) {
        Optional<? extends GenomicVariant> genoVarOptional;
        if (liftToGrch37) {
            if (variant.isBreakend()) {
                genoVarOptional = lift.liftBreakend(((GenomicBreakendVariant) variant));
            } else {
                genoVarOptional = lift.liftIntrachromosomal(variant);
            }
        } else {
            genoVarOptional = Optional.of(variant);
        }
        return genoVarOptional;
    }
}
