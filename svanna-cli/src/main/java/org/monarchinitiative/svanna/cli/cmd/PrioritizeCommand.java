package org.monarchinitiative.svanna.cli.cmd;

import org.monarchinitiative.svanna.cli.Main;
import org.monarchinitiative.svanna.cli.writer.*;
import org.monarchinitiative.svanna.cli.writer.html.AnalysisParameters;
import org.monarchinitiative.svanna.configuration.exception.InvalidResourceException;
import org.monarchinitiative.svanna.configuration.exception.MissingResourceException;
import org.monarchinitiative.svanna.configuration.exception.UndefinedResourceException;
import org.monarchinitiative.svanna.cli.writer.html.HtmlResultWriter;
import org.monarchinitiative.svanna.core.SvAnna;
import org.monarchinitiative.svanna.core.configuration.DataProperties;
import org.monarchinitiative.svanna.core.configuration.PrioritizationProperties;
import org.monarchinitiative.svanna.core.configuration.SvAnnaProperties;
import org.monarchinitiative.svanna.core.filter.PopulationFrequencyAndCoverageFilter;
import org.monarchinitiative.svanna.core.io.VariantParser;
import org.monarchinitiative.svanna.core.priority.SvPrioritizer;
import org.monarchinitiative.svanna.core.priority.SvPrioritizerFactory;
import org.monarchinitiative.svanna.core.priority.SvPriority;
import org.monarchinitiative.svanna.core.service.AnnotationDataService;
import org.monarchinitiative.svanna.core.service.PhenotypeDataService;
import org.monarchinitiative.svanna.io.FullSvannaVariant;
import org.monarchinitiative.svanna.io.parse.VcfVariantParser;
import org.monarchinitiative.svanna.model.landscape.variant.PopulationVariantOrigin;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.phenopackets.phenopackettools.io.PhenopacketParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@CommandLine.Command(name = "prioritize",
        header = "Prioritize the variants.",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class PrioritizeCommand extends SvAnnaCommand {

    protected static final NumberFormat NF = NumberFormat.getNumberInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritizeCommand.class);

    static {
        NF.setMaximumFractionDigits(2);
    }

    @CommandLine.ArgGroup(validate = false, heading = "Analysis input:%n")
    public InputOptions inputOptions = new InputOptions();
    public static class InputOptions {
        @CommandLine.Option(names = {"-p", "--phenopacket"},
                description = "Path to v1 or v2 phenopacket in JSON, YAML or Protobuf format.")
        public Path phenopacket = null;

        @CommandLine.Option(names = {"-t", "--phenotype-term"},
                description = "HPO term ID(s). Can be provided multiple times.")
        public List<String> hpoTermIdList = null;

        @CommandLine.Option(names = {"--vcf"},
                description = "Path to the input VCF file.")
        public Path vcf = null;
    }

    @CommandLine.ArgGroup(validate = false, heading = "Run options:%n")
    public RunOptions runOptions = new RunOptions();
    public static class RunOptions {
        /*
         * ------------ FILTERING OPTIONS ------------
         */
        @CommandLine.Option(names = {"--frequency-threshold"},
                description = "Frequency threshold as a percentage [0-100] (default: ${DEFAULT-VALUE}).")
        public float frequencyThreshold = 1.F;

        @CommandLine.Option(names = {"--overlap-threshold"},
                description = "Percentage threshold for determining variant's region is similar enough to database entry (default: ${DEFAULT-VALUE}).")
        public float overlapThreshold = 80.F;

        @CommandLine.Option(names = {"--min-read-support"},
                description = "Minimum number of ALT reads to prioritize (default: ${DEFAULT-VALUE}).")
        public int minAltReadSupport = 3;

        @CommandLine.Option(names = {"--n-threads"},
                paramLabel = "2",
                description = "Process variants using n threads (default: ${DEFAULT-VALUE}).")
        public int parallelism = 2;
    }

    @CommandLine.ArgGroup(validate = false, heading = "Output options:%n")
    public OutputConfig outputConfig = new OutputConfig();
    public static class OutputConfig {
        @CommandLine.Option(names = {"--no-breakends"},
                description = "Do not include breakend variants into HTML report (default: ${DEFAULT-VALUE}).")
        public boolean doNotReportBreakends = false;

        @CommandLine.Option(names = {"--output-format"},
                paramLabel = "html",
                description = "Comma separated list of output formats to use for writing the results (default: ${DEFAULT-VALUE}).")
        public String outputFormats = "html";

        @CommandLine.Option(names = {"--out-dir"},
            description = "Path to folder where to write the output files (default: current working directory).")
        public Path outDir = Path.of("");

        @CommandLine.Option(names = {"--prefix"},
                description = "Prefix for output files (default: based on the input VCF name).")
        public String outPrefix = null;

        @CommandLine.Option(names = {"--report-top-variants"},
                paramLabel = "100",
                description = "Report top n variants (default: ${DEFAULT-VALUE}).")
        public int reportNVariants = 100;

        @CommandLine.Option(names = {"--uncompressed-output"},
                description = "Write tabular and VCF output formats with no compression (default: ${DEFAULT-VALUE}).")
        public boolean uncompressed = false;
    }

    @Override
    public Integer execute() {
        int status = checkArguments();
        if (status != 0)
            return status;

        PrioritizationProperties prioritizationProperties = prioritizationProperties();
        DataProperties dataProperties = dataProperties();
        SvAnnaProperties svAnnaProperties = SvAnnaProperties.of(svannaDataDirectory, prioritizationProperties, dataProperties);

        try {
            AnalysisData analysisData = parseAnalysisData();
            runAnalysis(analysisData, svAnnaProperties);
        } catch (InterruptedException | ExecutionException | IOException | InvalidResourceException |
                 MissingResourceException | UndefinedResourceException | AnalysisInputException e) {
            LOGGER.error("Error: {}", e.getMessage());
            LOGGER.debug("Error: {}", e.getMessage(), e);
            return 1;
        }

        LOGGER.info("We're done, bye!");
        return 0;
    }

    private AnalysisData parseAnalysisData() throws AnalysisInputException {
        if (inputOptions.hpoTermIdList != null) { // CLI
            LOGGER.info("Using {} phenotype features supplied via CLI", inputOptions.hpoTermIdList.size());
            Path vcf = inputOptions.vcf;
            List<TermId> phenotypeTermIds = inputOptions.hpoTermIdList.stream()
                    .map(TermId::of)
                    .collect(Collectors.toList());
            return new AnalysisData(phenotypeTermIds, vcf);
        } else { // Phenopacket
            LOGGER.info("Using phenotype features from a phenopacket at {}", inputOptions.phenopacket.toAbsolutePath());
            PhenopacketParserFactory parserFactory = PhenopacketParserFactory.getInstance();

            // try v2 first
            try {
                LOGGER.debug("Trying v2 format first..");
                AnalysisData analysisData = PhenopacketAnalysisDataUtil.parseV2Phenopacket(inputOptions.phenopacket, inputOptions.vcf, parserFactory);
                LOGGER.debug("Success!");
                return analysisData;
            } catch (AnalysisInputException e) {
                // swallow and try v1
                LOGGER.debug("Unable to decode {} as v2 phenopacket, falling back to v1", inputOptions.phenopacket.toAbsolutePath());
            }

            // try v1 or fail
            AnalysisData analysisData = PhenopacketAnalysisDataUtil.parseV1Phenopacket(inputOptions.phenopacket, inputOptions.vcf, parserFactory);
            LOGGER.debug("Success!");
            return analysisData;
        }

    }

    protected int checkArguments() {
        if (inputOptions.hpoTermIdList == null && inputOptions.phenopacket == null) {
            LOGGER.error("No phenotype features provided. Use the CLI or a phenopacket");
            return 1;
        }

        if (inputOptions.hpoTermIdList != null && inputOptions.phenopacket != null) {
            LOGGER.error("Passing HPO terms both through CLI and Phenopacket is not supported. Choose one");
            return 1;
        }

        if (inputOptions.vcf == null && inputOptions.phenopacket == null) {
            LOGGER.error("Path to a VCF file or to a phenopacket must be supplied");
            return 1;
        }

        if (runOptions.parallelism < 1) {
            LOGGER.error("Thread number must be positive: {}", runOptions.parallelism);
            return 1;
        }
        int processorsAvailable = Runtime.getRuntime().availableProcessors();
        if (runOptions.parallelism > processorsAvailable) {
            LOGGER.warn("You asked for more threads ({}) than processors ({}) available on the system", runOptions.parallelism, processorsAvailable);
        }

        if (outputConfig.outputFormats.isEmpty()) {
            LOGGER.error("Aborting the analysis since no valid output format was provided");
            return 1;
        }

        if (!Files.isDirectory(outputConfig.outDir)) {
            LOGGER.info("The output directory {} does not exist. Creating the missing directories.", outputConfig.outDir.toAbsolutePath());
            try {
                Files.createDirectories(outputConfig.outDir);
            } catch (IOException e) {
                LOGGER.error("Unable to creating the output directory: {}", e.getMessage());
                return 1;
            }
        }
        return 0;
    }

    // TODO - improve the exception handling
    private void runAnalysis(AnalysisData analysisData, SvAnnaProperties svAnnaProperties) throws IOException, ExecutionException, InterruptedException, InvalidResourceException, MissingResourceException, UndefinedResourceException {
        Collection<OutputFormat> outputFormats = Utils.parseOutputFormats(outputConfig.outputFormats);
        SvAnna svAnna = bootstrapSvAnna(svAnnaProperties);

        GenomicAssembly genomicAssembly = svAnna.assembly();

        // check that the HPO terms entered by the user (if any) are valid
        PhenotypeDataService phenotypeDataService = svAnna.phenotypeDataService();

        LOGGER.debug("Validating the provided phenotype terms");
        Set<Term> validatedPatientTerms = phenotypeDataService.validateTerms(analysisData.phenotypeTerms());
        LOGGER.debug("Preparing the top-level phenotype terms for the input terms");
        Set<Term> topLevelHpoTerms = phenotypeDataService.getTopLevelTerms(validatedPatientTerms);

        LOGGER.info("Reading variants from `{}`", analysisData.vcf());
        VariantParser<FullSvannaVariant> parser = new VcfVariantParser(genomicAssembly);
        List<FullSvannaVariant> variants = parser.createVariantAlleleList(analysisData.vcf());
        LOGGER.info("Read {} variants", NF.format(variants.size()));

        // Filter
        LOGGER.info("Filtering out the variants with reciprocal overlap >{}% occurring in more than {}% probands", runOptions.overlapThreshold, runOptions.frequencyThreshold);
        LOGGER.info("Filtering out the variants where ALT allele is supported by less than {} reads", runOptions.minAltReadSupport);
        AnnotationDataService annotationDataService = svAnna.annotationDataService();
        PopulationFrequencyAndCoverageFilter filter = new PopulationFrequencyAndCoverageFilter(annotationDataService, runOptions.overlapThreshold, runOptions.frequencyThreshold, runOptions.minAltReadSupport);
        List<FullSvannaVariant> filteredVariants = filter.filter(variants);

        // Prioritize
        SvPrioritizerFactory svPrioritizerFactory = svAnna.prioritizerFactory();
        SvPrioritizer<SvPriority> prioritizer = svPrioritizerFactory.getPrioritizer(analysisData.phenotypeTerms());

        LOGGER.info("Prioritizing {} variants on {} threads", NF.format(filteredVariants.size()), runOptions.parallelism);
        ProgressReporter priorityProgress = new ProgressReporter(5_000);
        UnaryOperator<FullSvannaVariant> prioritizationFunction = v -> {
            priorityProgress.logItem(v);
            SvPriority priority = prioritizer.prioritize(v.genomicVariant());
            v.setSvPriority(priority);
            return v;
        };
        Instant start = Instant.now();
        List<FullSvannaVariant> filteredPrioritizedVariants = TaskUtils.executeBlocking(filteredVariants, prioritizationFunction, runOptions.parallelism);

        long totalMilliseconds = Duration.between(start, Instant.now()).toMillis();
        String elapsedSeconds = NF.format((totalMilliseconds / 1000) / 60 % 60);
        String elapsedMilliseconds = NF.format(totalMilliseconds / 1000 % 60);
        String averageItemsPerSecond = NF.format(((double) filteredVariants.size() / totalMilliseconds) * 1000.);
        LOGGER.info("Prioritization finished in {}m {}s ({} ms) processing on average {} items/s",
                elapsedSeconds, elapsedMilliseconds, NF.format(totalMilliseconds), averageItemsPerSecond);

        AnalysisResults results = new AnalysisResults(analysisData.vcf().toAbsolutePath().toString(), validatedPatientTerms, topLevelHpoTerms, filteredPrioritizedVariants);

        LOGGER.info("Writing out the results");
        ResultWriterFactory resultWriterFactory = resultWriterFactory(svAnna);
        String prefix = resolveOutPrefix(analysisData.vcf());
        OutputOptions outputOptions = new OutputOptions(outputConfig.outDir, prefix, outputConfig.reportNVariants);
        for (OutputFormat outputFormat : outputFormats) {
            ResultWriter writer = resultWriterFactory.resultWriterForFormat(outputFormat, !outputConfig.uncompressed);
            if (writer instanceof HtmlResultWriter) {
                // TODO - is there a more elegant way to pass the HTML specific parameters into the writer?
                HtmlResultWriter htmlWriter = (HtmlResultWriter) writer;
                htmlWriter.setAnalysisParameters(getAnalysisParameters(analysisData, svAnnaProperties));
                htmlWriter.setDoNotReportBreakends(outputConfig.doNotReportBreakends);
            }
            writer.write(results, outputOptions);
        }

    }

    private String resolveOutPrefix(Path vcfFile) {
        if (outputConfig.outPrefix != null && !outputConfig.outPrefix.isBlank())
            return outputConfig.outPrefix;

        String vcfName = vcfFile.toFile().getName();
        String prefixBase;
        if (vcfName.endsWith(".vcf.gz"))
            prefixBase = vcfName.substring(0, vcfName.length() - 7);
        else if (vcfName.endsWith(".vcf"))
            prefixBase = vcfName.substring(0, vcfName.length() - 4);
        else
            prefixBase = vcfName;
        return prefixBase + ".SVANNA";
    }

    private AnalysisParameters getAnalysisParameters(AnalysisData analysisData, SvAnnaProperties properties) {
        AnalysisParameters analysisParameters = new AnalysisParameters();

        analysisParameters.setDataDirectory(properties.dataDirectory().toAbsolutePath().toString());
        analysisParameters.setPhenopacketPath(inputOptions.phenopacket == null ? null : inputOptions.phenopacket.toAbsolutePath().toString());
        analysisParameters.setVcfPath(analysisData.vcf().toAbsolutePath().toString());
        analysisParameters.setSimilarityThreshold(runOptions.overlapThreshold);
        analysisParameters.setFrequencyThreshold(runOptions.frequencyThreshold);
        analysisParameters.addAllPopulationVariantOrigins(PopulationVariantOrigin.benign());
        analysisParameters.setMinAltReadSupport(runOptions.minAltReadSupport);
        analysisParameters.setTadStabilityThreshold(properties.dataProperties().tadStabilityThresholdAsPercentage());
        analysisParameters.setUseVistaEnhancers(properties.dataProperties().useVista());
        analysisParameters.setUseFantom5Enhancers(properties.dataProperties().useFantom5());
        analysisParameters.setPhenotypeTermSimilarityMeasure(properties.prioritizationProperties().termSimilarityMeasure().toString());

        return analysisParameters;
    }

}
