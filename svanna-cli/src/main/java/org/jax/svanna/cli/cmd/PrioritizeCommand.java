package org.jax.svanna.cli.cmd;

import org.jax.svanna.cli.writer.*;
import org.jax.svanna.configuration.exception.InvalidResourceException;
import org.jax.svanna.configuration.exception.MissingResourceException;
import org.jax.svanna.configuration.exception.UndefinedResourceException;
import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.writer.html.AnalysisParameters;
import org.jax.svanna.cli.writer.html.HtmlResultWriter;
import org.jax.svanna.core.SvAnna;
import org.jax.svanna.core.configuration.DataProperties;
import org.jax.svanna.core.configuration.PrioritizationProperties;
import org.jax.svanna.core.configuration.SvAnnaProperties;
import org.jax.svanna.core.filter.PopulationFrequencyAndCoverageFilter;
import org.jax.svanna.core.io.VariantParser;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPrioritizerFactory;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.service.AnnotationDataService;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.jax.svanna.io.FullSvannaVariant;
import org.jax.svanna.io.parse.VcfVariantParser;
import org.jax.svanna.model.landscape.variant.PopulationVariantOrigin;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.HtsFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
                description = "Path to phenopacket.")
        public Path phenopacket = null;

        @CommandLine.Option(names = {"-t", "--phenotype-term"},
                description = "HPO term ID(s). Can be provided multiple times.")
        public List<String> hpoTermIdList = List.of();

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
    public Integer call() {
        int status = checkArguments();
        if (status != 0)
            return status;

        PrioritizationProperties prioritizationProperties = prioritizationProperties();
        DataProperties dataProperties = dataProperties();
        SvAnnaProperties svAnnaProperties = SvAnnaProperties.of(svannaDataDirectory, prioritizationProperties, dataProperties);

        Optional<AnalysisData> analysisData = parseAnalysisData();
        if (analysisData.isEmpty())
            return 1;

        try {
            runAnalysis(analysisData.get(), svAnnaProperties);
        } catch (InterruptedException | ExecutionException | IOException | InvalidResourceException |
                 MissingResourceException | UndefinedResourceException e) {
            LOGGER.error("Error: {}", e.getMessage());
            LOGGER.debug("Error: {}", e.getMessage(), e);
            return 1;
        }

        LOGGER.info("We're done, bye!");
        return 0;
    }

    private Optional<AnalysisData> parseAnalysisData() {
        Path vcf;
        List<TermId> phenotypeTermIds;
        if (inputOptions.vcf != null) { // VCF & CLI
            vcf = inputOptions.vcf;
            phenotypeTermIds = inputOptions.hpoTermIdList.stream()
                    .map(TermId::of)
                    .collect(Collectors.toList());
        } else { // phenopacket
            try {
                Phenopacket phenopacket = PhenopacketImporter.readPhenopacket(inputOptions.phenopacket);
                phenotypeTermIds = phenopacket.getPhenotypicFeaturesList().stream()
                        .map(pf -> TermId.of(pf.getType().getId()))
                        .collect(Collectors.toList());

                Optional<Path> vcfFilePathOptional = getVcfFilePath(phenopacket);
                if (vcfFilePathOptional.isEmpty()) {
                    if (inputOptions.vcf == null) {
                        LOGGER.error("VCF file was found neither in CLI arguments nor in the Phenopacket. Aborting.");
                        return Optional.empty();
                    } else {
                        vcf = inputOptions.vcf;
                    }
                } else {
                    LOGGER.info("VCF file was found in both CLI arguments and in the Phenopacket. Using the file from CLI: `{}`", inputOptions.vcf);
                    vcf = inputOptions.vcf;
                }

            } catch (IOException e) {
                LOGGER.error("Error reading phenopacket at `{}`: {}", inputOptions.phenopacket, e.getMessage());
                return Optional.empty();
            }
        }

        return Optional.of(new AnalysisData(phenotypeTermIds, vcf));
    }

    private static Optional<Path> getVcfFilePath(Phenopacket phenopacket) {
        // There should be exactly one VCF file
        LinkedList<HtsFile> vcfFiles = phenopacket.getHtsFilesList().stream()
                .filter(htsFile -> htsFile.getHtsFormat().equals(HtsFile.HtsFormat.VCF))
                .distinct()
                .collect(Collectors.toCollection(LinkedList::new));
        if (vcfFiles.isEmpty()) {
            LOGGER.info("VCF file was not found in Phenopacket. Expecting to find the file among the CLI arguments");
            return Optional.empty();
        }

        if (vcfFiles.size() > 1)
            LOGGER.warn("Found >1 VCF files. Using the first one.");

        // The VCF file should have a proper URI
        HtsFile vcf = vcfFiles.getFirst();
        try {
            URI uri = new URI(vcf.getUri());
            return Optional.of(Path.of(uri));
        } catch (URISyntaxException e) {
            LOGGER.warn("Invalid URI `{}`: {}", vcf.getUri(), e.getMessage());
            return Optional.empty();
        }
    }

    protected int checkArguments() {
        if ((inputOptions.vcf == null) == (inputOptions.phenopacket == null)) {
            LOGGER.error("Path to a VCF file or to a phenopacket must be supplied");
            return 1;
        }

        if (inputOptions.phenopacket != null && !inputOptions.hpoTermIdList.isEmpty()) {
            LOGGER.error("Passing HPO terms both through CLI and Phenopacket is not supported");
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
        OutputOptions outputOptions = new OutputOptions(prefix, outputConfig.reportNVariants);
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
        if (outputConfig.outPrefix != null)
            return outputConfig.outPrefix;

        String vcfPath = vcfFile.toAbsolutePath().toString();
        String prefixBase;
        if (vcfPath.endsWith(".vcf.gz"))
            prefixBase = vcfPath.substring(0, vcfPath.length() - 7);
        else if (vcfPath.endsWith(".vcf"))
            prefixBase = vcfPath.substring(0, vcfPath.length() - 4);
        else
            prefixBase = vcfPath;
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


    private static class AnalysisData {
        private final List<TermId> phenotypeTerms;
        private final Path vcf;

        private AnalysisData(List<TermId> phenotypeTerms, Path vcf) {
            this.phenotypeTerms = phenotypeTerms;
            this.vcf = vcf;
        }

        public List<TermId> phenotypeTerms() {
            return phenotypeTerms;
        }

        public Path vcf() {
            return vcf;
        }
    }
}
