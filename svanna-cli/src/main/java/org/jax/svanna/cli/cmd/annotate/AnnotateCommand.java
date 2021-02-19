package org.jax.svanna.cli.cmd.annotate;


import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.cmd.PoolUtils;
import org.jax.svanna.cli.cmd.ProgressReporter;
import org.jax.svanna.cli.cmd.SvAnnaCommand;
import org.jax.svanna.cli.html.FilterAndCount;
import org.jax.svanna.cli.html.HtmlTemplate;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.filter.Filter;
import org.jax.svanna.core.filter.StructuralVariantFrequencyFilter;
import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.PopulationVariantDao;
import org.jax.svanna.core.overlap.Overlapper;
import org.jax.svanna.core.overlap.SvAnnOverlapper;
import org.jax.svanna.core.priority.DbPrototypeSvPrioritizer;
import org.jax.svanna.core.priority.DiscreteSvPriority;
import org.jax.svanna.core.priority.SvImpact;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.TranscriptService;
import org.jax.svanna.core.viz.HtmlVisualizer;
import org.jax.svanna.core.viz.VisualizableGenerator;
import org.jax.svanna.core.viz.VisualizableGeneratorSimple;
import org.jax.svanna.core.viz.Visualizer;
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

import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
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

    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE})")
    public String outprefix = "SVANNA";

    @CommandLine.Option(names = {"-v", "--vcf"})
    public Path vcfFile = null;

    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list)")
    public List<String> hpoTermIdList;

    @CommandLine.Option(names = {"--threshold"}, type = SvImpact.class, description = "report variants as severe as this or more")
    public SvImpact threshold = SvImpact.HIGH;

    @CommandLine.Option(names = {"-max_genes"}, description = "maximum gene count to prioritize an SV (default: ${DEFAULT-VALUE})")
    public int maxGenes = 100;

    @CommandLine.Option(names = {"--similarity-threshold"}, description = "percentage threshold for determining variant's region is similar enough to database entry (default: ${DEFAULT-VALUE})")
    public float similarityThreshold = 80.F;

    @CommandLine.Option(names = {"--frequency-threshold"}, description = "frequency threshold as a percentage [0-100] (default: ${DEFAULT-VALUE})")
    public float frequencyThreshold = 1.F;

    @CommandLine.Option(names={"--min-read-support"}, description="Minimum number of ALT reads to prioritize (default: ${DEFAULT-VALUE})")
    public int minAltReadSupport = 2;

    @CommandLine.Option(names = {"--n-threads"}, paramLabel = "2", description = "Process variants using n threads (default: ${DEFAULT-VALUE})")
    public int nThreads = 2;

    @CommandLine.Option(names = {"-n", "--report-top-variants"}, paramLabel = "50", description = "Report top n variants (default: ${DEFAULT-VALUE})")
    public int reportNVariants = 100;

    @CommandLine.Option(names={"-p","--phenopacket"}, description = "phenopacket with HPO terms and path to VCF file")
    public Path phenopacketPath = null;

    private static Comparator<? super SvannaVariant> prioritizedVariantComparator() {
        return (l, r) -> {
            int priority = r.svPriority().compareTo(l.svPriority()); // the order is intentional
            if (priority != 0)
                return priority;
            return Variant.compare(l, r);
        };
    }

    @Override
    public Integer call() throws Exception {
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

        LogUtils.logInfo(LOGGER, "Running `annotate` command...");

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

            AnnotationDataService annotationDataService = context.getBean(AnnotationDataService.class);

            LogUtils.logDebug(LOGGER, "Preparing top-level enhancer phenotype terms for the input terms");
            Set<TermId> enhancerTerms = annotationDataService.enhancerPhenotypeAssociations();
            Set<TermId> enhancerRelevantAncestors = phenotypeDataService.getRelevantAncestors(enhancerTerms, patientTerms);

            // gene symbols
            LogUtils.logDebug(LOGGER, "Preparing gene and disease data");
            Map<String, GeneWithId> geneSymbolMap = phenotypeDataService.geneBySymbol();
            TranscriptService transcriptService = context.getBean(TranscriptService.class);
            // disease summary map
            Map<TermId, Set<HpoDiseaseSummary>> relevantGenesAndDiseases = phenotypeDataService.getRelevantGenesAndDiseases(patientTerms);


            LogUtils.logInfo(LOGGER, "Filtering out variants with reciprocal overlap >{}% occurring in more than {}% probands", similarityThreshold, frequencyThreshold);
            PopulationVariantDao populationVariantDao = context.getBean(PopulationVariantDao.class);
            Filter<SvannaVariant> variantFilter = new StructuralVariantFrequencyFilter(populationVariantDao, similarityThreshold, frequencyThreshold);

            // setup prioritization parts
            Overlapper overlapper = new SvAnnOverlapper(transcriptService.getChromosomeMap());

            SvPrioritizer<Variant, DiscreteSvPriority> prioritizer = new DbPrototypeSvPrioritizer(annotationDataService,
                    overlapper,
                    geneSymbolMap,
//                    topLevelHpoTermsAndLabels.keySet(),
                    enhancerRelevantAncestors,
                    relevantGenesAndDiseases,
                    maxGenes);

            // process variants
            LogUtils.logInfo(LOGGER, "Reading variants from `{}`", vcfFile);
            VariantParser<SvannaVariant> parser = new VcfVariantParser(genomicAssembly, false);
            List<SvannaVariant> variants = parser.createVariantAlleleList(vcfFile);
            LogUtils.logInfo(LOGGER, "Read {} variants", NF.format(variants.size()));

            LogUtils.logInfo(LOGGER, "Filtering and prioritizing variants");
            ProgressReporter progressReporter = new ProgressReporter(5_000);
            List<SvannaVariant> filteredPrioritizedVariants;
            try (Stream<SvannaVariant> stream = variants.stream()) {
                Stream<SvannaVariant> variantStream = stream.parallel()
                        .peek(progressReporter::logItem).onClose(progressReporter.summarize())
                        .peek(v -> v.addFilterResult(variantFilter.runFilter(v)))
                        .peek(v -> v.setSvPriority(prioritizer.prioritize(v)));


                ForkJoinPool workerPool = PoolUtils.makePool(nThreads);
                ForkJoinTask<List<SvannaVariant>> task = workerPool.submit(() -> variantStream.collect(Collectors.toList()));
                filteredPrioritizedVariants = task.get();
                workerPool.shutdown();
            } catch (InterruptedException | ExecutionException e) {
                LogUtils.logError(LOGGER, "Error: {}", e.getMessage());
                throw e;
            }

            int above = 0, below = 0;
            double thresholdValue = threshold.priority();
            for (SvannaVariant variant : filteredPrioritizedVariants) {
                if (variant.svPriority().getPriority() >= thresholdValue) {
                    above += 1;
                } else {
                    below += 1;
                }
            }
            LogUtils.logInfo(LOGGER, "Above threshold SVs: {}, below threshold SVs: {}", NF.format(above), NF.format(below));

            // TODO -- if we have frequency information
            // svList - svann.prioritizeSvsByPopulationFrequency(svList);
            // This filters our SVs with lower impact than our threshold

            List<DiscreteSvPriority> priorities = filteredPrioritizedVariants.stream().map(SvannaVariant::svPriority).collect(Collectors.toList());
            FilterAndCount fac = new FilterAndCount(priorities, filteredPrioritizedVariants, threshold, minAltReadSupport);
            int unparsableCount = fac.getUnparsableCount();

            Map<String, String> infoMap = new HashMap<>();
            infoMap.put("vcf_file", vcfFile == null ? "" : vcfFile.toString());
            infoMap.put("unparsable", String.valueOf(unparsableCount));
            infoMap.put("n_affectedGenes", String.valueOf(fac.getnAffectedGenes()));
            infoMap.put("n_affectedEnhancers", String.valueOf(fac.getnAffectedEnhancers()));
            infoMap.put("counts_table", fac.toHtmlTable());
            infoMap.put("phenopacket_file", phenopacketPath == null ? "" : phenopacketPath.toAbsolutePath().toString());

            // setup visualization parts
            VisualizableGenerator graphicsGenerator = new VisualizableGeneratorSimple(overlapper, annotationDataService, phenotypeDataService);
            Visualizer visualizer = new HtmlVisualizer();
            LogUtils.logInfo(LOGGER, "Reporting {} variants sorted by priority", reportNVariants);
            List<String> visualizations = filteredPrioritizedVariants.stream()
                    .filter(vp -> vp.numberOfAltReads() >= minAltReadSupport && vp.passedFilters())
                    .sorted(prioritizedVariantComparator())
                    .map(graphicsGenerator::makeVisualizable)
                    .map(visualizer::getHtml)
                    .limit(reportNVariants)
                    .collect(Collectors.toList());

            HtmlTemplate template = new HtmlTemplate(visualizations, infoMap, topLevelHpoTerms, validatedPatientTerms);
            template.outputFile(outprefix);

            return 0;
        }
    }

}
