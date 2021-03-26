package org.jax.svanna.cli.cmd.annotate;


import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.cmd.*;
import org.jax.svanna.cli.writer.AnalysisResults;
import org.jax.svanna.cli.writer.OutputFormat;
import org.jax.svanna.cli.writer.ResultWriter;
import org.jax.svanna.cli.writer.ResultWriterFactory;
import org.jax.svanna.cli.writer.html.HtmlResultFormatParameters;
import org.jax.svanna.cli.writer.html.HtmlResultWriter;
import org.jax.svanna.core.analysis.FilterAndPrioritize;
import org.jax.svanna.core.analysis.VariantAnalysis;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.filter.Filter;
import org.jax.svanna.core.filter.StructuralVariantFrequencyFilter;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.overlap.SvAnnOverlapper;
import org.jax.svanna.core.priority.StrippedSvPrioritizer;
import org.jax.svanna.core.priority.SvImpact;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.TranscriptService;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(name = "annotate",
        aliases = {"A"},
        header = "Annotate a VCF file",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class AnnotateCommand extends SvAnnaCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateCommand.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    static {
        NF.setMaximumFractionDigits(2);
    }

    /*
     * ------------ ANALYSIS OPTIONS ------------
     */
    @CommandLine.Option(names = {"--vcf"})
    public Path vcfFile = null;

    @CommandLine.Option(names = {"-p", "--phenopacket"}, description = "phenopacket with HPO terms and path to VCF file")
    public Path phenopacketPath = null;

    @CommandLine.Option(names = {"--n-threads"}, paramLabel = "2", description = "Process variants using n threads (default: ${DEFAULT-VALUE})")
    public int nThreads = 2;

    @CommandLine.Option(names = {"-max_genes"}, description = "maximum gene count to prioritize an SV (default: ${DEFAULT-VALUE})")
    public int maxGenes = 100;


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
        LogUtils.logWarn(LOGGER, "\n\nTHIS COMMAND IS CURRENTLY NOT WORKING\n\n");

        if ((vcfFile == null) == (phenopacketPath == null)) {
            LogUtils.logWarn(LOGGER,"Provide either path to a VCF file or path to a phenopacket (not both)");
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
            List<TermId> patientTerms;
            if (phenopacketPath != null) {
                PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketPath, phenotypeDataService.ontology());
                patientTerms = importer.getHpoTerms();
                vcfFile = importer.getVcfPath();
            } else {
                patientTerms = hpoTermIdList.stream().map(TermId::of).collect(Collectors.toList());
            }

            LogUtils.logDebug(LOGGER, "Validating provided phenotype terms");
            Set<Term> validatedPatientTerms = phenotypeDataService.validateTerms(patientTerms);
            LogUtils.logDebug(LOGGER, "Preparing top-level phenotype terms for the input terms");
            Set<Term> topLevelHpoTerms = phenotypeDataService.getTopLevelTerms(validatedPatientTerms);


            LogUtils.logInfo(LOGGER, "Reading variants from `{}`", vcfFile);
            VariantParser<SvannaVariant> parser = new VcfVariantParser(genomicAssembly, false);
            List<SvannaVariant> variants = parser.createVariantAlleleList(vcfFile);
            LogUtils.logInfo(LOGGER, "Read {} variants", NF.format(variants.size()));


            LogUtils.logInfo(LOGGER, "Setting up filtering and prioritization");
            TranscriptService transcriptService = context.getBean(TranscriptService.class);
            AnnotationDataService annotationDataService = context.getBean(AnnotationDataService.class);
            VariantAnalysis<SvannaVariant> variantAnalysis = setupVariantAnalysis(patientTerms, transcriptService, annotationDataService, phenotypeDataService);


            LogUtils.logInfo(LOGGER, "Filtering and prioritizing variants");
            AnalysisResults results;
            ProgressReporter progressReporter = new ProgressReporter(5_000);
            try (Stream<SvannaVariant> variantStream = variants.parallelStream()
                    .peek(progressReporter::logItem)
                    .onClose(progressReporter.summarize())) {
                Stream<SvannaVariant> prioritized = variantAnalysis.analyze(variantStream);

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
                    HtmlResultFormatParameters parameters = new HtmlResultFormatParameters(reportNVariants, minAltReadSupport);
                    ((HtmlResultWriter) writer).setParameters(parameters);
                }
                writer.write(results, outprefix);
            }

            return 0;
        } catch (Exception e) {
            LogUtils.logError(LOGGER, "Error occurred: {}", e.getMessage());
            return 1;
        }
    }

    private VariantAnalysis<SvannaVariant> setupVariantAnalysis(Collection<TermId> patientTerms,
                                                                TranscriptService transcriptService,
                                                                AnnotationDataService annotationDataService,
                                                                PhenotypeDataService phenotypeDataService) {
        // setup filtering
        LogUtils.logInfo(LOGGER, "Filtering out variants with reciprocal overlap >{}% occurring in more than {}% probands", similarityThreshold, frequencyThreshold);
        Filter<SvannaVariant> variantFilter = new StructuralVariantFrequencyFilter(annotationDataService, similarityThreshold, frequencyThreshold);

        LogUtils.logDebug(LOGGER, "Preparing top-level enhancer phenotype terms for the input terms");
        Set<TermId> enhancerTerms = annotationDataService.enhancerPhenotypeAssociations();
        Set<TermId> enhancerRelevantAncestors = phenotypeDataService.getRelevantAncestors(enhancerTerms, patientTerms);

        LogUtils.logDebug(LOGGER, "Preparing gene and disease data");
        Map<TermId, Set<HpoDiseaseSummary>> relevantGenesAndDiseases = phenotypeDataService.getRelevantGenesAndDiseases(patientTerms);

        // setup prioritization parts
        SvPrioritizer<SvannaVariant, ? extends SvPriority> prioritizer = new StrippedSvPrioritizer(annotationDataService,
                new SvAnnOverlapper(transcriptService.getChromosomeMap()),
                phenotypeDataService.geneBySymbol(),
//                    topLevelHpoTermsAndLabels.keySet(),
                enhancerRelevantAncestors,
                relevantGenesAndDiseases,
                maxGenes);

        return new FilterAndPrioritize<>(variantFilter, prioritizer);
    }

}
