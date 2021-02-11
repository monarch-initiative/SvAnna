package org.jax.svanna.cli.cmd.annotate;


import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.cmd.SvAnnaCommand;
import org.jax.svanna.cli.html.FilterAndCount;
import org.jax.svanna.cli.html.HtmlTemplate;
import org.jax.svanna.core.annotation.AnnotationDataService;
import org.jax.svanna.core.annotation.PopulationVariantDao;
import org.jax.svanna.core.filter.Filter;
import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.filter.StructuralVariantFrequencyFilter;
import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.overlap.EnhancerOverlapper;
import org.jax.svanna.core.overlap.Overlapper;
import org.jax.svanna.core.overlap.SvAnnOverlapper;
import org.jax.svanna.core.prioritizer.PrototypeSvPrioritizer;
import org.jax.svanna.core.prioritizer.SvImpact;
import org.jax.svanna.core.prioritizer.SvPrioritizer;
import org.jax.svanna.core.prioritizer.SvPriority;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.TranscriptService;
import org.jax.svanna.core.viz.HtmlVisualizable;
import org.jax.svanna.core.viz.HtmlVisualizer;
import org.jax.svanna.core.viz.Visualizable;
import org.jax.svanna.core.viz.Visualizer;
import org.jax.svanna.io.hpo.HpoDiseaseGeneMap;
import org.jax.svanna.io.parse.VariantParser;
import org.jax.svanna.io.parse.VcfVariantParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@CommandLine.Command(name = "annotate",
        aliases = {"A"},
        header = "annotate VCF file",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class AnnotateCommand extends SvAnnaCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateCommand.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    @CommandLine.Option(names = {"-d", "--data-dir"}, description = "directory with data, downloaded by `download` subcommand (default: ${DEFAULT-VALUE})")
    public Path dataDir = Paths.get("data");

//    @CommandLine.Option(names = {"-g", "--gencode"})
//    public Path geneCodePath = Paths.get("data/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz");

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

    @CommandLine.Option(names={"-p","--phenopacket"}, description = "phenopacket with HPO terms and path to VCF file")
    public Path phenopacketPath = null;

    private static IntervalEndExtractor<Enhancer> intervalEndExtractor() {
        return new IntervalEndExtractor<>() {
            @Override
            public int getBegin(Enhancer x) {
                return x.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
            }

            @Override
            public int getEnd(Enhancer x) {
                return x.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
            }
        };
    }

    @Override
    public Integer call() throws Exception {
        if ((vcfFile == null) == (phenopacketPath == null)) {
            LOGGER.warn("Provide either path to a VCF file or path to a phenopacket (not both)");
            return 1;
        }
        LOGGER.info("Running `annotate` command...");
        // 0 - set up data

        // TODO 8.11.2020, note we need to get the HPO Ontology object to translate the HP term ids that are provided
        //  by the user into their corresponding labels on the output file.
        //  I will add a method to this class for now, but when we refactor this, we should make it more elegant
        HpoDiseaseGeneMap hpoDiseaseGeneMap = HpoDiseaseGeneMap.loadGenesAndDiseaseMap(dataDir);
        Ontology hpo = hpoDiseaseGeneMap.getOntology();
        List<TermId>  patientTerms;
        if (phenopacketPath != null) {
            PhenopacketImporter importer = PhenopacketImporter.fromJson(phenopacketPath, hpo);
            patientTerms = importer.getHpoTerms();
            vcfFile = importer.getVcfPath();
        } else {
            patientTerms = hpoTermIdList.stream().map(TermId::of).collect(Collectors.toList());
        }
        // check that the HPO terms entered by the user (if any) are valid
        Map<TermId, String> topLevelHpoTermsAndLabels;
        Map<TermId, String> originalHpoTermsAndLabels;
        if (!patientTerms.isEmpty()) {
            HpoTopLevel hpoTopLevel = new HpoTopLevel(patientTerms, hpo);
            topLevelHpoTermsAndLabels = hpoTopLevel.getUpperLevelTerms();
            originalHpoTermsAndLabels = hpoTopLevel.getOriginalTerms();
        } else {
            topLevelHpoTermsAndLabels = Map.of();
            originalHpoTermsAndLabels = Map.of();
        }

        Map<Integer, IntervalArray<Enhancer>> enhancerMap = new HashMap<>();
        Set<TermId> enhancerTerms;
        try (ConfigurableApplicationContext context = getContext()) {
            GenomicAssembly genomicAssembly = context.getBean(GenomicAssembly.class);

            AnnotationDataService annotationDataService = context.getBean(AnnotationDataService.class);

            List<Enhancer> enhancers = annotationDataService.allEnhancers();
            enhancerTerms = enhancers.stream()
                    .map(Enhancer::hpoTermAssociations)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            Map<Integer, List<Enhancer>> enhancersByContig = enhancers.stream()
                    .collect(Collectors.groupingBy(Enhancer::contigId));
            for (Map.Entry<Integer, List<Enhancer>> entry : enhancersByContig.entrySet()) {
                enhancerMap.put(entry.getKey(), new IntervalArray<>(entry.getValue(), intervalEndExtractor()));
            }

//            TSpecParser tparser = new TSpecParser(enhancerFile, genomicAssembly);
//        Map<Integer, IntervalArray<SomeEnhancer>> enhancerMap = tparser.getChromosomeToEnhancerIntervalArrayMap();
//        Set<TermId> enhancerRelevantAncestors = hpoDiseaseGeneMap.getRelevantAncestors(tparser.getId2enhancerMap().keySet(), patientTerms);
            Set<TermId> enhancerRelevantAncestors = hpoDiseaseGeneMap.getRelevantAncestors(enhancerTerms, patientTerms);

            // gene symbols
            Map<String, GeneWithId> geneSymbolMap = hpoDiseaseGeneMap.getGeneSymbolMap();
            TranscriptService transcriptService = context.getBean(TranscriptService.class);
            // disease summary map
            Map<TermId, Set<HpoDiseaseSummary>> relevantGenesAndDiseases = hpoDiseaseGeneMap.getRelevantGenesAndDiseases(patientTerms);


            LOGGER.info("Filtering out variants with reciprocal overlap >{}% occurring in more than {}% probands", similarityThreshold, frequencyThreshold);
            PopulationVariantDao populationVariantDao = context.getBean(PopulationVariantDao.class);
            Filter<SvannaVariant> variantFilter = new StructuralVariantFrequencyFilter(populationVariantDao, similarityThreshold, frequencyThreshold);

            // setup prioritization parts
            Overlapper overlapper = new SvAnnOverlapper(transcriptService.getChromosomeMap());
            EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(enhancerMap);

            SvPrioritizer<Variant> prioritizer = new PrototypeSvPrioritizer(overlapper,
                    enhancerOverlapper,
                    geneSymbolMap,
                    topLevelHpoTermsAndLabels.keySet(),
                    enhancerRelevantAncestors,
                    relevantGenesAndDiseases,
                    maxGenes);
            List<SvPriority> priorities = new ArrayList<>(); // where to store the prioritization results
            // setup visualization parts
            Visualizer visualizer = new HtmlVisualizer();

            // read variants
            VariantParser<SvannaVariant> parser = new VcfVariantParser(genomicAssembly, false);
            List<SvannaVariant> variants = parser.createVariantAlleleList(vcfFile);
            List<PrioritizedVariant> prioritizedVariants = new LinkedList<>();
            int above = 0, below = 0;
            for (SvannaVariant variant : variants) {
                // run prioritization
                SvPriority priority = prioritizer.prioritize(variant);
                priorities.add(priority);
                if (priority.getImpact().satisfiesThreshold(threshold)) {
                    above++;
                    // run filtering
                    FilterResult filterResult = variantFilter.runFilter(variant);
                    variant.addFilterResult(filterResult);
                    prioritizedVariants.add(new PrioritizedVariant(variant, priority));
                } else {
                    below++;
                }
            }
            LOGGER.info("Above threshold SVs: {}, below threshold SVs: {}", NF.format(above), NF.format(below));

            // TODO -- if we have frequency information
            // svList - svann.prioritizeSvsByPopulationFrequency(svList);
            // This filters our SVs with lower impact than our threshold

            FilterAndCount fac = new FilterAndCount(priorities, variants, threshold, minAltReadSupport);
            int unparsableCount = fac.getUnparsableCount();

            Map<String, String> infoMap = new HashMap<>();
            infoMap.put("vcf_file", vcfFile == null ? "" : vcfFile.toString());
            infoMap.put("unparsable", String.valueOf(unparsableCount));
            infoMap.put("n_affectedGenes", String.valueOf(fac.getnAffectedGenes()));
            infoMap.put("n_affectedEnhancers", String.valueOf(fac.getnAffectedEnhancers()));
            infoMap.put("counts_table", fac.toHtmlTable());
            infoMap.put("phenopacket_file", phenopacketPath == null ? "" : phenopacketPath.toAbsolutePath().toString());

            List<String> visualizations = new ArrayList<>();
            Collections.sort(prioritizedVariants);
            for (var pr : prioritizedVariants) {
                if (pr.variant().numberOfAltReads() < minAltReadSupport) {
                    continue;
                } else if (! pr.variant().passedFilters()) {
                    continue;
                }
                Visualizable vizbell = pr.getVisualizable();
                visualizations.add(visualizer.getHtml(vizbell));
            }

            HtmlTemplate template = new HtmlTemplate(visualizations,
                    infoMap,
                    topLevelHpoTermsAndLabels,
                    originalHpoTermsAndLabels);
            template.outputFile(outprefix);

            // We're done!
            return 0;
        }
    }

    /**
     * An inner class that is designed for ssorting the prioritized structural variants acccording to
     * (1) impact, (2) chromosome, and (3) position. For translocations, we take the "first" chromosome.
     */
    private static class PrioritizedVariant implements Comparable<PrioritizedVariant> {
        private final SvannaVariant structuralVariant;
        private final SvPriority priority;
        private final SvImpact impact;
        /** leftmost chromosome */
        private final Contig contig;
        /** leftmost (5') position */
        private final int position;


        private PrioritizedVariant(SvannaVariant sv, SvPriority priority) {
            this.structuralVariant = sv;
            this.priority = priority;
            this.impact = priority.getImpact();
            this.contig = sv.contig();
            this.position = sv.start();
        }
        public SvannaVariant variant() {
            return structuralVariant;
        }
        public SvPriority priority() {
            return priority;
        }

        public HtmlVisualizable getVisualizable() {
            return new HtmlVisualizable(structuralVariant, priority);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrioritizedVariant that = (PrioritizedVariant) o;
            return Objects.equals(structuralVariant, that.structuralVariant) &&
                    Objects.equals(priority, that.priority);
        }
        @Override
        public int hashCode() {
            return Objects.hash(structuralVariant, priority);
        }

        @Override
        public String toString() {
            return "PrioritizedVariant{" +
                    "structuralVariant=" + structuralVariant +
                    ", priority=" + priority +
                    ", impact=" + impact +
                    ", contig=" + contig +
                    ", position=" + position +
                    '}';
        }

        @Override
        public int compareTo(PrioritizedVariant that) {
            int priorityComparison = that.impact.compareTo(impact);
            if (priorityComparison != 0) {
                return priorityComparison;
            }
            int chromosomeComparison = Integer.compare(contig.id(), that.contig.id());
            if (chromosomeComparison != 0) {
                return chromosomeComparison;
            }
            return Integer.compare(position, that.position);
        }
    }

}
