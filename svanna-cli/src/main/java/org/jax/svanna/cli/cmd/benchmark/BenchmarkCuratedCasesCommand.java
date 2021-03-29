package org.jax.svanna.cli.cmd.benchmark;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.cmd.SvAnnaCommand;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.filter.PopulationFrequencyAndRepetitiveRegionFilter;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.priority.SvPriorityFactory;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.io.parse.VariantParser;
import org.jax.svanna.io.parse.VcfVariantParser;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "benchmark-curated-cases",
        aliases = {"BCC"},
        header = "Annotate cases with additive prioritizer features",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class BenchmarkCuratedCasesCommand extends SvAnnaCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateCasesCommand.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    @CommandLine.Option(names = {"--similarity-threshold"}, description = "percentage threshold for determining variant's region is similar enough to database entry (default: ${DEFAULT-VALUE})")
    public float similarityThreshold = 80.F;

    @CommandLine.Option(names = {"--frequency-threshold"}, description = "frequency threshold as a percentage [0-100] (default: ${DEFAULT-VALUE})")
    public float frequencyThreshold = 1.F;

    @CommandLine.Option(
            names = {"--vcf"},
            required = true,
            description = "path to VCF file with variants to be used as neutral background")
    public Path vcfFile = null;


    @CommandLine.Option(names = {"-f", "--case-folder"}, description = "path to a folder with case report JSON files")
    public Path caseReportPath;

    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE})")
    public String outPrefix = "SVANNA_PC";

    @CommandLine.Option(names = {"--n-threads"}, paramLabel = "2", description = "Process variants using n threads (default: ${DEFAULT-VALUE})")
    public int nThreads = 2;

    @CommandLine.Parameters(description = "path(s) to JSON files with case reports curated by Hpo Case Annotator")
    public List<Path> caseReports;


    @Override
    public Integer call() {
        if (vcfFile == null) {
            LogUtils.logError(LOGGER, "Path to VCF file must not be null");
            return 1;
        }
        List<CaseReport> cases = CaseReportImporter.readCaseReports(caseReports, caseReportPath);

        try (ConfigurableApplicationContext context = getContext()) {
            GenomicAssembly genomicAssembly = context.getBean(GenomicAssembly.class);

            LogUtils.logInfo(LOGGER, "Reading variants from `{}`", vcfFile);
            VariantParser<SvannaVariant> parser = new VcfVariantParser(genomicAssembly, false);
            List<SvannaVariant> variants = parser.createVariantAlleleList(vcfFile);
            LogUtils.logInfo(LOGGER, "Read {} variants", NF.format(variants.size()));

            LogUtils.logInfo(LOGGER, "Filtering out the variants with reciprocal overlap >{}% occurring in more than {}% probands", similarityThreshold, frequencyThreshold);
            LogUtils.logInfo(LOGGER, "Filtering out the variants where at least >{}% of variant's region occurs in a repetitive region", similarityThreshold);
            AnnotationDataService annotationDataService = context.getBean(AnnotationDataService.class);
            PopulationFrequencyAndRepetitiveRegionFilter filter = new PopulationFrequencyAndRepetitiveRegionFilter(annotationDataService, similarityThreshold, frequencyThreshold);

            SvPriorityFactory svPriorityFactory = context.getBean(SvPriorityFactory.class);

            PrioritizationRunner prioritizationRunner = new PrioritizationRunner(filter, svPriorityFactory, nThreads);

            PhenotypeDataService phenotypeDataService = context.getBean(PhenotypeDataService.class);

            int processed = 1;
            Map<String, List<VariantPriority>> results = new HashMap<>();
            Map<String, Set<String>> causalVariantIds = new HashMap<>();
            for (CaseReport aCase : cases) {
                LogUtils.logInfo(LOGGER, "({}/{}) Processing case `{}`", processed, cases.size(), aCase.caseName());

                // validate patient terms
                Set<TermId> validatedPatientTermIds = phenotypeDataService.validateTerms(aCase.patientTerms()).stream()
                        .map(Term::getId)
                        .collect(Collectors.toSet());

                // prepare the variants
                List<SvannaVariant> caseVariants = new LinkedList<>(variants);
                Collection<SvannaVariant> targetVariants = aCase.variants();
                caseVariants.addAll(targetVariants);
                List<VariantPriority> priorities = prioritizationRunner.prioritize(validatedPatientTermIds, caseVariants);

                results.put(aCase.caseName(), priorities);
                Set<String> causalIds = targetVariants.stream()
                        .map(Variant::id)
                        .collect(Collectors.toSet());
                causalVariantIds.put(aCase.caseName(), causalIds);

                processed++;
            }

            try {
                writeOutTheResults(results, causalVariantIds);
            } catch (IOException e) {
                LogUtils.logError(LOGGER, "Error: {}", e);
                return 1;
            }

            return 0;
        } catch (Exception e) {
            LogUtils.logError(LOGGER, "Error occurred: {}", e.getMessage());
            return 1;
        }
    }

    private void writeOutTheResults(Map<String, List<VariantPriority>> results, Map<String, Set<String>> causalVariantIds) throws IOException {
        Path outputPath = Path.of(outPrefix + ".csv.gz");
        LogUtils.logInfo(LOGGER, "Writing out the results to `{}`", outputPath.toAbsolutePath());
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GzipCompressorOutputStream(new FileOutputStream(outputPath.toFile()))))) {
            CSVPrinter printer = CSVFormat.DEFAULT
                    .withHeader("CASE_NAME", "VARIANT_ID", "VTYPE", "IS_CAUSAL", "PRIORITY")
                    .print(writer);

            for (Map.Entry<String, List<VariantPriority>> aCase : results.entrySet()) {
                String caseName = aCase.getKey();
                LogUtils.logTrace(LOGGER, "Writing the results for `{}`", caseName);
                Set<String> variantIds = causalVariantIds.get(caseName);
                for (VariantPriority variantPriority : aCase.getValue()) {
                    Variant variant = variantPriority.variant();

                    printer.print(caseName); // CASE_NAME
                    printer.print(variant.id()); // VARIANT_ID
                    printer.print(variant.variantType()); // VTYPE
                    printer.print(variantIds.contains(variant.id())); // IS_CAUSAL
                    printer.print(variantPriority.priority().getPriority()); // PRIORITY

                    printer.println();
                }
            }
        }
    }

}
