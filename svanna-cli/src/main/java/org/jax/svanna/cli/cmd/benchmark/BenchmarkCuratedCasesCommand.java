package org.jax.svanna.cli.cmd.benchmark;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.cmd.ProgressReporter;
import org.jax.svanna.cli.cmd.TaskUtils;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.filter.PopulationFrequencyAndCoverageFilter;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPrioritizerFactory;
import org.jax.svanna.core.priority.SvPrioritizerType;
import org.jax.svanna.core.priority.SvPriority;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "benchmark-curated-cases",
        aliases = {"BCC"},
        header = "Annotate cases with additive prioritizer features",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class BenchmarkCuratedCasesCommand extends BaseBenchmarkCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateCasesCommand.class);

    @CommandLine.Option(
            names = {"--vcf"},
            required = true,
            description = "path to VCF file with variants to be used as neutral background")
    public Path vcfFile = null;

    @CommandLine.Option(names = {"-f", "--case-folder"}, description = "path to a folder with case report JSON files")
    public Path caseReportPath;

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
            LogUtils.logInfo(LOGGER, "Filtering out the variants where ALT allele is supported by less than {} reads", minAltReadSupport);
            AnnotationDataService annotationDataService = context.getBean(AnnotationDataService.class);
            PopulationFrequencyAndCoverageFilter filter = new PopulationFrequencyAndCoverageFilter(annotationDataService, similarityThreshold, frequencyThreshold, minAltReadSupport);
            List<SvannaVariant> allVariants = filter.filter(variants);

            List<SvannaVariant> filteredVariants = allVariants.stream()
                    .filter(SvannaVariant::passedFilters)
                    .collect(Collectors.toList());

            LogUtils.logInfo(LOGGER, "Removed {} variants that failed the filtering", variants.size() - filteredVariants.size());

            PhenotypeDataService phenotypeDataService = context.getBean(PhenotypeDataService.class);
            SvPrioritizerFactory svPrioritizerFactory = context.getBean(SvPrioritizerFactory.class);

            int processed = 1;
            Map<String, List<VariantPriority>> results = new HashMap<>();
            Map<String, Set<String>> causalVariantIds = new HashMap<>();
            for (CaseReport aCase : cases) {
                LogUtils.logInfo(LOGGER, "({}/{}) Processing case `{}`", processed, cases.size(), aCase.caseSummary());

                // get and validate patient terms
                Collection<TermId> patientTerms = aCase.patientTerms();
                Set<Term> validatedPatientTerms = phenotypeDataService.validateTerms(patientTerms);
                Set<TermId> validatedPatientTermIds = validatedPatientTerms.stream().map(Term::getId).collect(Collectors.toSet());

                // create the prioritizer seeded by the phenotype terms and prioritize the variants
                SvPrioritizer<SvPriority> prioritizer = svPrioritizerFactory.getPrioritizer(SvPrioritizerType.ADDITIVE, validatedPatientTermIds);

                // prepare the variants
                List<Variant> caseVariants = new LinkedList<>(filteredVariants);
                Collection<SvannaVariant> targetVariants = aCase.variants();
                List<SvannaVariant> filteredTargetVariants = filter.filter(targetVariants);
                for (SvannaVariant filteredTargetVariant : filteredTargetVariants) {
                    if (filteredTargetVariant.passedFilters())
                        caseVariants.add(filteredTargetVariant);
                    else
                        LogUtils.logWarn(LOGGER, "Variant {}-{} did not pass the filters!", aCase.caseSummary(), LogUtils.variantSummary(filteredTargetVariant));
                }

                ProgressReporter progressReporter = new ProgressReporter(5_000);
                Stream<VariantPriority> annotationStream = caseVariants.parallelStream()
                        .onClose(progressReporter.summarize())
                        .peek(progressReporter::logItem)
                        .map(v -> new VariantPriority(v, prioritizer.prioritize(v)));
                List<VariantPriority> priorities = TaskUtils.executeBlocking(() -> annotationStream.collect(Collectors.toList()), nThreads);

                results.put(aCase.caseSummary().caseSummary(), priorities);
                Set<String> causalIds = targetVariants.stream()
                        .map(Variant::id)
                        .collect(Collectors.toSet());
                causalVariantIds.put(aCase.caseSummary().caseSummary(), causalIds);

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
