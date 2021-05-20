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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "benchmark-case",
        aliases = {"BC"},
        header = "Annotate case with additive prioritizer features",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class BenchmarkCaseCommand extends BaseBenchmarkCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkCaseCommand.class);

    @CommandLine.Option(
            names = {"-o", "--output"},
            description="Where to write the output"
    )
    public Path outputPath;

    @CommandLine.Parameters(
            index = "0",
            description = "path to VCF file with variants to be used as neutral background")
    public Path vcfFile;

    @CommandLine.Parameters(
            index = "1",
            description = "path to Phenopacket JSON file with case reports curated by Hpo Case Annotator")
    public Path caseReport;


    @Override
    public Integer call() throws Exception {
        List<CaseReport> cases = CaseReportImporter.readCasesProvidedAsPositionalArguments(List.of(caseReport));

        if (cases.isEmpty()) {
            LogUtils.logWarn(LOGGER, "Unable to continue with no cases `{}`");
            return 1;
        }

        CaseReport caseReport = cases.get(0);

        try (ConfigurableApplicationContext context = getContext()) {
            GenomicAssembly genomicAssembly = context.getBean(GenomicAssembly.class);

            LogUtils.logInfo(LOGGER, "Reading variants from `{}`", vcfFile);
            VariantParser<SvannaVariant> parser = new VcfVariantParser(genomicAssembly, false);
            List<SvannaVariant> variants = parser.createVariantAlleleList(vcfFile);
            LogUtils.logInfo(LOGGER, "Read {} variants", NF.format(variants.size()));

            LogUtils.logInfo(LOGGER, "Filtering out the variants with reciprocal overlap >{}% occurring in more than {}% probands", similarityThreshold, frequencyThreshold);
            LogUtils.logInfo(LOGGER, "Filtering out the variants where ALT allele is supported by less than {} reads", minAltReadSupport);
            AnnotationDataService annotationDataService = context.getBean(AnnotationDataService.class);
            PopulationFrequencyAndCoverageFilter filter = new PopulationFrequencyAndCoverageFilter(annotationDataService, similarityThreshold, frequencyThreshold, minAltReadSupport, maxLength);
            List<SvannaVariant> allVariants = filter.filter(variants);

            List<SvannaVariant> filteredVariants = allVariants.stream()
                    .filter(SvannaVariant::passedFilters)
                    .collect(Collectors.toList());

            LogUtils.logInfo(LOGGER, "Removed {} variants that failed the filtering", variants.size() - filteredVariants.size());

            PhenotypeDataService phenotypeDataService = context.getBean(PhenotypeDataService.class);
            SvPrioritizerFactory svPrioritizerFactory = context.getBean(SvPrioritizerFactory.class);

            // ---------------------------------------------

            // get and validate patient terms
            Collection<TermId> patientTerms = caseReport.patientTerms();
            Set<Term> validatedPatientTerms = phenotypeDataService.validateTerms(patientTerms);
            Set<TermId> validatedPatientTermIds = validatedPatientTerms.stream().map(Term::getId).collect(Collectors.toSet());

            // create the prioritizer seeded by the phenotype terms and prioritize the variants
            SvPrioritizer<SvPriority> prioritizer = svPrioritizerFactory.getPrioritizer(SvPrioritizerType.ADDITIVE, validatedPatientTermIds);

            // prepare the variants
            List<Variant> caseVariants = new LinkedList<>(filteredVariants);
            Collection<SvannaVariant> targetVariants = caseReport.variants();
            List<SvannaVariant> filteredTargetVariants = filter.filter(targetVariants);
            for (SvannaVariant filteredTargetVariant : filteredTargetVariants) {
                if (filteredTargetVariant.passedFilters())
                    caseVariants.add(filteredTargetVariant);
                else
                    LogUtils.logWarn(LOGGER, "Variant {}-{} did not pass the filters!", caseReport.caseSummary(), LogUtils.variantSummary(filteredTargetVariant));
            }

            ProgressReporter progressReporter = new ProgressReporter(5_000);
            Stream<VariantPriority> annotationStream = caseVariants.parallelStream()
                    .onClose(progressReporter.summarize())
                    .peek(progressReporter::logItem)
                    .map(v -> new VariantPriority(v, prioritizer.prioritize(v)));
            List<VariantPriority> priorities = TaskUtils.executeBlocking(() -> annotationStream.collect(Collectors.toList()), nThreads);

            Set<String> causalIds = targetVariants.stream()
                    .map(Variant::id)
                    .collect(Collectors.toSet());

            // ---------------------------------------------

            try {
                writeOutTheResults(caseReport.caseSummary().caseSummary(), vcfFile.toFile().getName(), priorities, causalIds);
            } catch (IOException e) {
                LogUtils.logError(LOGGER, "Error: {}", e);
                return 1;
            }

            return 0;
        } catch (Exception e) {
            return 1;
        }

    }

    private void writeOutTheResults(String caseName, String vcfName, List<VariantPriority> priorities, Set<String> causalVariantIds) throws IOException {
        Path output = (outputPath != null)
                ? outputPath
                : Path.of(outPrefix + ".csv.gz");
        LogUtils.logInfo(LOGGER, "Writing the results for `{}` to ", caseName, output.toAbsolutePath());

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GzipCompressorOutputStream(new FileOutputStream(output.toFile()))))) {
            CSVPrinter printer = CSVFormat.DEFAULT
                    .withHeader("CASE_NAME", "VCF_NAME", "VARIANT_ID", "VTYPE", "IS_CAUSAL", "PRIORITY")
                    .print(writer);
            for (VariantPriority variantPriority : priorities) {
                Variant variant = variantPriority.variant();

                printer.print(caseName); // CASE_NAME
                printer.print(vcfName); // VCF_NAME
                printer.print(variant.id()); // VARIANT_ID
                printer.print(variant.variantType()); // VTYPE
                printer.print(causalVariantIds.contains(variant.id())); // IS_CAUSAL
                printer.print(variantPriority.priority().getPriority()); // PRIORITY

                printer.println();
            }
        }
    }

}
