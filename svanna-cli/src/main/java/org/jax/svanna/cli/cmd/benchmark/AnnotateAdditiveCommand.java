package org.jax.svanna.cli.cmd.benchmark;


import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.cmd.ProgressReporter;
import org.jax.svanna.cli.cmd.SvAnnaCommand;
import org.jax.svanna.cli.cmd.TaskUtils;
import org.jax.svanna.cli.cmd.Utils;
import org.jax.svanna.cli.writer.AnalysisResults;
import org.jax.svanna.cli.writer.OutputFormat;
import org.jax.svanna.cli.writer.ResultWriter;
import org.jax.svanna.cli.writer.ResultWriterFactory;
import org.jax.svanna.cli.writer.html.HtmlResultFormatParameters;
import org.jax.svanna.cli.writer.html.HtmlResultWriter;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.filter.Filter;
import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.RepetitiveRegionVariantFilter;
import org.jax.svanna.core.filter.StructuralVariantFrequencyFilter;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.priority.*;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.io.parse.VariantParser;
import org.jax.svanna.io.parse.VcfVariantParser;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "annotate-additive",
        aliases = {"AA"},
        header = "Prioritize the variants with additive prioritizer",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class AnnotateAdditiveCommand extends SvAnnaCommand {


    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateAdditiveCommand.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    /*
     * ------------ ANALYSIS OPTIONS ------------
     */
    @CommandLine.Option(names = {"-v", "--vcf"})
    public Path vcfFile = null;

    @CommandLine.Option(names = {"--n-threads"}, paramLabel = "2", description = "Process variants using n threads (default: ${DEFAULT-VALUE})")
    public int nThreads = 2;

    /*
     * ------------ FILTERING OPTIONS ------------
     */
    @CommandLine.Option(names = {"--similarity-threshold"}, description = "percentage threshold for determining variant's region is similar enough to database entry (default: ${DEFAULT-VALUE})")
    public float similarityThreshold = 80.F;

    @CommandLine.Option(names = {"--frequency-threshold"}, description = "frequency threshold as a percentage [0-100] (default: ${DEFAULT-VALUE})")
    public float frequencyThreshold = 1.F;

    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list)")
    public List<String> hpoTermIdList;


    /*
     * ------------  OUTPUT OPTIONS  ------------
     */
    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE})")
    public String outprefix = "SVANNA";

    @CommandLine.Option(names = {"-f", "--output-format"},
            paramLabel = "html",
            description = "Comma separated list of output formats to use for writing the results (default: ${DEFAULT-VALUE})")
    public String outputFormats = "html";

    @CommandLine.Option(names = {"--threshold"}, type = SvImpact.class, description = "report variants as severe as this or more")
    public SvImpact threshold = SvImpact.HIGH;

    @CommandLine.Option(names = {"-n", "--report-top-variants"}, paramLabel = "50", description = "Report top n variants (default: ${DEFAULT-VALUE})")
    public int reportNVariants = 100;

    @CommandLine.Option(names={"--min-read-support"}, description="Minimum number of ALT reads to prioritize (default: ${DEFAULT-VALUE})")
    public int minAltReadSupport = 2;


    @Override
    public Integer call() {
        if ((vcfFile == null)) {
            LogUtils.logWarn(LOGGER,"Path to a VCF file must be supplied");
            return 1;
        }

        if (nThreads < 1) {
            LogUtils.logError(LOGGER, "Thread number must be positive: {}", nThreads);
            return 1;
        }
        int processorsAvailable = Runtime.getRuntime().availableProcessors();
        if (nThreads > processorsAvailable) {
            LogUtils.logWarn(LOGGER, "You asked for more threads ({}) than processors ({}) available on the system", nThreads, processorsAvailable);
        }

        Collection<OutputFormat> outputFormats = Utils.parseOutputFormats(this.outputFormats);
        if (outputFormats.isEmpty()) {
            LogUtils.logWarn(LOGGER, "Aborting the analysis since no valid output format was provided");
            return 0;
        }

        try (ConfigurableApplicationContext context = getContext()) {
            GenomicAssembly genomicAssembly = context.getBean(GenomicAssembly.class);

            // check that the HPO terms entered by the user (if any) are valid
            PhenotypeDataService phenotypeDataService = context.getBean(PhenotypeDataService.class);
            List<TermId> patientTerms = hpoTermIdList.stream().map(TermId::of).collect(Collectors.toList());

            LogUtils.logDebug(LOGGER, "Validating the provided phenotype terms");
            Set<Term> validatedPatientTerms = phenotypeDataService.validateTerms(patientTerms);
            LogUtils.logDebug(LOGGER, "Preparing the top-level phenotype terms for the input terms");
            Set<Term> topLevelHpoTerms = phenotypeDataService.getTopLevelTerms(validatedPatientTerms);

            LogUtils.logInfo(LOGGER, "Reading variants from `{}`", vcfFile);
            VariantParser<SvannaVariant> parser = new VcfVariantParser(genomicAssembly, false);
            List<SvannaVariant> variants = parser.createVariantAlleleList(vcfFile);
            LogUtils.logInfo(LOGGER, "Read {} variants", NF.format(variants.size()));

            // filter
            LogUtils.logInfo(LOGGER, "Filtering out the variants with reciprocal overlap >{}% occurring in more than {}% probands", similarityThreshold, frequencyThreshold);
            AnnotationDataService annotationDataService = context.getBean(AnnotationDataService.class);
            Filter<SvannaVariant> variantFilter = new StructuralVariantFrequencyFilter(annotationDataService, similarityThreshold, frequencyThreshold);
            LogUtils.logInfo(LOGGER, "Filtering out the variants where at least >{}% of variant's region occurs in a repetitive region", similarityThreshold);
            Filter<SvannaVariant> repetitiveRegionFilter = new RepetitiveRegionVariantFilter(annotationDataService, similarityThreshold);

            LogUtils.logInfo(LOGGER, "Filtering variants");
            ProgressReporter filterProgress = new ProgressReporter(5_000);
            List<SvannaVariant> filteredVariants;
            try (Stream<SvannaVariant> filteredStream = variants.parallelStream()
                    .peek(filterProgress::logItem)
                    .onClose(filterProgress.summarize())) {
                Stream<SvannaVariant> filtered = filteredStream.peek(v -> {
                    FilterResult pfFv = variantFilter.runFilter(v);
                    v.addFilterResult(pfFv);
                    FilterResult rrFr = repetitiveRegionFilter.runFilter(v);
                    v.addFilterResult(rrFr);
                });
                filteredVariants = TaskUtils.executeBlocking(() -> filtered.collect(Collectors.toList()), nThreads);
            } catch (InterruptedException | ExecutionException e) {
                LogUtils.logError(LOGGER, "Error: {}", e.getMessage());
                return 1;
            }

            // Prioritize
            SvPriorityFactory svPriorityFactory = context.getBean(SvPriorityFactory.class);
            SvPrioritizer<SvannaVariant, SvPriority> prioritizer = svPriorityFactory.getPrioritizer(SvPrioritizerType.ADDITIVE, patientTerms);

            LogUtils.logInfo(LOGGER, "Prioritizing variants");
            AnalysisResults results;
            ProgressReporter priorityProgress = new ProgressReporter(5_000);
            try (Stream<SvannaVariant> variantStream = filteredVariants.parallelStream()
                    .peek(priorityProgress::logItem)
                    .onClose(priorityProgress.summarize())) {
                Stream<SvannaVariant> prioritized = variantStream.peek(v -> {
                    SvPriority priority = prioritizer.prioritize(v);
                    v.setSvPriority(priority);
                });

                List<SvannaVariant> filteredPrioritizedVariants = TaskUtils.executeBlocking(() -> prioritized.collect(Collectors.toList()), nThreads);

                results = new AnalysisResults(vcfFile.toAbsolutePath().toString(), validatedPatientTerms, topLevelHpoTerms, filteredPrioritizedVariants);
            } catch (InterruptedException | ExecutionException e) {
                LogUtils.logError(LOGGER, "Error: {}", e.getMessage());
                return 1;
            }

            ResultWriterFactory resultWriterFactory = context.getBean(ResultWriterFactory.class);
            for (OutputFormat outputFormat : outputFormats) {
                ResultWriter writer = resultWriterFactory.resultWriterForFormat(outputFormat);
                if (writer instanceof HtmlResultWriter) {
                    // TODO - is there a more elegant way to pass the HTML specific parameters into the writer?
                    HtmlResultFormatParameters parameters = new HtmlResultFormatParameters(threshold, reportNVariants, minAltReadSupport);
                    ((HtmlResultWriter) writer).setParameters(parameters);
                }
                writer.write(results, outprefix);
            }
        } catch (Exception e) {
            LogUtils.logError(LOGGER, "Error occurred: {}", e.getMessage(), e);
            return 1;
        }

        LogUtils.logInfo(LOGGER, "The analysis is complete. Bye");
        return 0;
    }

}
