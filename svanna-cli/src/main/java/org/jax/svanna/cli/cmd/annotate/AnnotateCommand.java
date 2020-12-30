package org.jax.svanna.cli.cmd.annotate;


import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.html.FilterAndCount;
import org.jax.svanna.cli.html.HtmlTemplate;
import org.jax.svanna.core.filter.AllPassFilter;
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
import org.jax.svanna.core.reference.GenomicAssemblyProvider;
import org.jax.svanna.core.reference.SvannaVariant;
import org.jax.svanna.core.reference.TranscriptService;
import org.jax.svanna.core.reference.transcripts.JannovarTranscriptService;
import org.jax.svanna.core.viz.HtmlVisualizable;
import org.jax.svanna.core.viz.HtmlVisualizer;
import org.jax.svanna.core.viz.Visualizable;
import org.jax.svanna.core.viz.Visualizer;
import org.jax.svanna.io.filter.dgv.DgvFeatureSource;
import org.jax.svanna.io.hpo.HpoDiseaseGeneMap;
import org.jax.svanna.io.hpo.TSpecParser;
import org.jax.svanna.io.parse.VariantParser;
import org.jax.svanna.io.parse.VcfVariantParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.variant.api.Contig;
import org.monarchinitiative.variant.api.GenomicAssembly;
import org.monarchinitiative.variant.api.Variant;
import org.monarchinitiative.variant.api.VariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "annotate",
        aliases = {"A"},
        header = "annotate VCF file",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class AnnotateCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateCommand.class);

    private static final NumberFormat NF = NumberFormat.getNumberInstance();

    @CommandLine.Option(names = {"-d", "--data-dir"}, description = "directory with data, downloaded by `download` subcommand (default: ${DEFAULT-VALUE})")
    public Path dataDir = Paths.get("data");

    @CommandLine.Option(names = {"-j", "--jannovar"}, description = "Jannovar transcript definition file (default: ${DEFAULT-VALUE} )")
    public Path jannovarPath = Paths.get("data/data/hg38_refseq_curated.ser");

    @CommandLine.Option(names = {"-g", "--gencode"})
    public Path geneCodePath = Paths.get("data/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz");

    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE})")
    public String outprefix = "SVANNA";

    @CommandLine.Option(names = {"-v", "--vcf"}, required = true)
    public Path vcfFile;

    @CommandLine.Option(names = {"-e", "--enhancer"}, description = "tspec enhancer file")
    public Path enhancerFile;

    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list)")
    public List<String> hpoTermIdList;

    @CommandLine.Option(names = {"--threshold"}, type = SvImpact.class, description = "report variants as severe as this or more")
    public SvImpact threshold = SvImpact.HIGH;

    @CommandLine.Option(names = {"-max_genes"}, description = "maximum gene count to prioritize an SV (default: ${DEFAULT-VALUE})")
    public int maxGenes = 100;

    @CommandLine.Option(names = {"--dgv-file"}, description = "DGV variant file")
    public Path dgvFile = null;

    @CommandLine.Option(names = {"--similarity-threshold"}, description = "percentage threshold for determining variant's region is similar enough to database entry (default: ${DEFAULT-VALUE})")
    public float similarityThreshold = 80.F;

    @CommandLine.Option(names = {"--frequency-threshold"}, description = "frequency threshold as a percentage [0-100] (default: ${DEFAULT-VALUE})")
    public float frequencyThreshold = 1.F;

    @CommandLine.Option(names={"--min-read-support"}, description="Minimum number of ALT reads to prioritize (default: ${DEFAULT-VALUE})")
    public int minAltReadSupport = 2;

    private static JannovarData readJannovarData(Path jannovarPath) throws SerializationException {
        return new JannovarDataSerializer(jannovarPath.toString()).load();
    }

    @Override
    public Integer call() throws Exception {
        LOGGER.info("Running `annotate` command...");
        // 0 - set up data

        // assembly
        Charset charset = StandardCharsets.UTF_8;
        GenomicAssembly assembly;
        try (InputStream is = Main.class.getResourceAsStream("/GCA_000001405.28_GRCh38.p13_assembly_report.txt")) {
            assembly = GenomicAssemblyProvider.fromAssemblyReport(is, charset);
        }

        // TODO 8.11.2020, note we need to get the HPO Ontology object to translate the HP term ids that are provided
        //  by the user into their corresponding labels on the output file.
        //  I will add a method to this class for now, but when we refactor this, we should make it more elegant
        HpoDiseaseGeneMap hpoDiseaseGeneMap = HpoDiseaseGeneMap.loadGenesAndDiseaseMap(dataDir);

        // patient phenotype
        Set<TermId> patientTerms = hpoTermIdList.stream().map(TermId::of).collect(Collectors.toSet());
        // check that the HPO terms entered by the user (if any) are valid
        Map<TermId, String> hpoTermsAndLabels;
        if (!patientTerms.isEmpty())
            hpoTermsAndLabels = hpoDiseaseGeneMap.getTermLabelMap(patientTerms);
        else
            hpoTermsAndLabels = Map.of();
        final Ontology hpo = hpoDiseaseGeneMap.getOntology();
        // enhancers & relevant enhancer terms
        TSpecParser tparser = new TSpecParser(enhancerFile, assembly);
        Map<Integer, IntervalArray<Enhancer>> enhancerMap = tparser.getChromosomeToEnhancerIntervalArrayMap();
        Set<TermId> enhancerRelevantAncestors = hpoDiseaseGeneMap.getRelevantAncestors(tparser.getId2enhancerMap().keySet(), patientTerms);
        // gene symbols
        Map<String, GeneWithId> geneSymbolMap = hpoDiseaseGeneMap.getGeneSymbolMap();
        // jannovar data
        JannovarData jannovarData = readJannovarData(jannovarPath);
        TranscriptService transcriptService = JannovarTranscriptService.of(assembly, jannovarData);
        // disease summary map
        Map<TermId, Set<HpoDiseaseSummary>> relevantGenesAndDiseases = hpoDiseaseGeneMap.getRelevantGenesAndDiseases(patientTerms);

        // 1 - parse input variants
        VariantParser<SvannaVariant> parser = new VcfVariantParser(assembly, false);
        List<SvannaVariant> variants = parser.createVariantAlleleList(vcfFile);

        // 2 - setup variant filtering
        Filter<SvannaVariant> variantFilter;
        if (dgvFile == null) {
            LOGGER.info("Variants will not be filtered");
            variantFilter = new AllPassFilter();
        } else {
            LOGGER.info("Filtering out variants with reciprocal overlap >{}% occurring in more than {}% probands of the DGV file at `{}`",
                    similarityThreshold, frequencyThreshold, dgvFile);
            DgvFeatureSource dgvFeatureSource = new DgvFeatureSource(assembly, dgvFile);
            variantFilter = new StructuralVariantFrequencyFilter(dgvFeatureSource, similarityThreshold, frequencyThreshold);
        }

        // 3 - prioritize & visualize variants
        // setup prioritization parts
        Overlapper overlapper = new SvAnnOverlapper(transcriptService.getChromosomeMap());
        EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(enhancerMap);

        SvPrioritizer<Variant> prioritizer = new PrototypeSvPrioritizer(overlapper,
                enhancerOverlapper,
                geneSymbolMap,
                patientTerms,
                enhancerRelevantAncestors,
                relevantGenesAndDiseases,
                maxGenes);
        List<SvPriority> priorities = new ArrayList<>(); // where to store the prioritization results
        // setup visualization parts
        Visualizer visualizer = new HtmlVisualizer();

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

        FilterAndCount fac = new FilterAndCount(priorities, variants, threshold, this.minAltReadSupport);
        int unparsableCount = fac.getUnparsableCount();

        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("vcf_file", vcfFile.toString());
        infoMap.put("unparsable", String.valueOf(unparsableCount));
        infoMap.put("n_affectedGenes", String.valueOf(fac.getnAffectedGenes()));
        infoMap.put("n_affectedEnhancers", String.valueOf(fac.getnAffectedEnhancers()));
        infoMap.put("counts_table", fac.toHtmlTable());

        List<String> visualizations = new ArrayList<>();
        Collections.sort(prioritizedVariants);
        for (var pr : prioritizedVariants) {
            if (pr.variant().numberOfAltReads() < this.minAltReadSupport) {
                continue;
            } else if (! pr.variant().passedFilters()) {
                continue;
            }
            Visualizable vizbell = pr.getVisualizable();
            visualizations.add(visualizer.getHtml(vizbell));
        }

        HtmlTemplate template = new HtmlTemplate(visualizations,
                infoMap,
                hpoTermsAndLabels);
        template.outputFile(outprefix);

        // We're done!
        return 0;
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
            int priorityComparison = impact.compareTo(that.impact);
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
