package org.jax.svanna.cli.cmd;


import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.cli.GenomicAssemblyProvider;
import org.jax.svanna.cli.Main;
import org.jax.svanna.cli.html.HtmlTemplate;
import org.jax.svanna.core.filter.AllPassFilter;
import org.jax.svanna.core.filter.Filter;
import org.jax.svanna.core.filter.FilterResult;
import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseGeneMap;
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
import org.jax.svanna.core.reference.transcripts.JannovarTranscriptService;
import org.jax.svanna.core.viz.HtmlVisualizable;
import org.jax.svanna.core.viz.HtmlVisualizer;
import org.jax.svanna.core.viz.Visualizable;
import org.jax.svanna.core.viz.Visualizer;
import org.jax.svanna.io.hpo.TSpecParser;
import org.jax.svanna.io.parse.VariantParser;
import org.jax.svanna.io.parse.VcfVariantParser;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.variant.api.Contig;
import org.monarchinitiative.variant.api.GenomicAssembly;
import org.monarchinitiative.variant.api.VariantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

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

    private static final Path ASSEMBLY_REPORT_PATH = Path.of(Main.class.getResource("/GCA_000001405.28_GRCh38.p13_assembly_report.txt").getPath());

    private final HpoDiseaseGeneMap hpoDiseaseGeneMap;
    @CommandLine.Option(names = {"-j", "--jannovar"}, description = "Jannovar transcript definition file (default: ${DEFAULT-VALUE} )")
    public Path jannovarPath = Paths.get("data/data/hg38_refseq_curated.ser");
    @CommandLine.Option(names = {"-g", "--gencode"})
    public Path geneCodePath = Paths.get("data/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz");
    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    public String outprefix = "SVANNA";
    @CommandLine.Option(names = {"-v", "--vcf"}, required = true)
    public Path vcfFile;
    @CommandLine.Option(names = {"-e", "--enhancer"}, description = "tspec enhancer file")
    public Path enhancerFile;
    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list)")
    public String hpoTermIdList;
    @CommandLine.Option(names = {"--threshold"}, type = SvImpact.class, description = "report variants as severe as this or more")
    public SvImpact threshold = SvImpact.HIGH;
    @CommandLine.Option(names = {"-max_genes"}, description = "maximum gene count to prioritize an SV (default: ${DEFAULT-VALUE} )")
    public int maxGenes = 100;

    public AnnotateCommand() {
        // TODO: 2. 11. 2020 externalize
        // TODO 8.11.2020, note we need to get the HPO Ontology object to translate the HP term ids
        // that are provided by the user into their corresponding labels on the output file.
        // I will add a method to this class for now, but when we refactor this, we should make it
        // more elegant
        hpoDiseaseGeneMap = HpoDiseaseGeneMap.loadGenesAndDiseaseMap();
    }

    private static JannovarData readJannovarData(Path jannovarPath) throws SerializationException {
        return new JannovarDataSerializer(jannovarPath.toString()).load();
    }

    @Override
    public Integer call() throws Exception {
        // 0 - set up data

        // assembly
        GenomicAssembly assembly = GenomicAssemblyProvider.fromAssemblyReport(ASSEMBLY_REPORT_PATH);

        // patient phenotype
        Set<TermId> patientTerms = Arrays.stream(hpoTermIdList.split(",")).map(String::trim).map(TermId::of).collect(Collectors.toSet());
        // check that the HPO terms entered by the user (if any) are valid
        Map<TermId, String> hpoTermsAndLabels;
        if (! patientTerms.isEmpty())
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
//        VariantParser<AnnotatedVariant> parser = new VcfVariantParser(assembly, false);
        VariantParser<SvannaVariant> parser = new VcfVariantParser(assembly, false);


        List<SvannaVariant> variants = parser.createVariantAlleleList(vcfFile);

        // 2 - setup variant filtering
        Filter<SvannaVariant> variantFilter = new AllPassFilter();

        // 3 - prioritize & visualize variants
        // setup prioritization parts
        Overlapper overlapper = new SvAnnOverlapper(transcriptService.getChromosomeMap());
        EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(enhancerMap);

        SvPrioritizer prioritizer = new PrototypeSvPrioritizer(overlapper,
                enhancerOverlapper,
                geneSymbolMap,
                patientTerms,
                enhancerRelevantAncestors,
                relevantGenesAndDiseases,
                maxGenes);
        List<SvPriority> priorities = new ArrayList<>(); // where to store the prioritization results
        // setup visualization parts
        Visualizer visualizer = new HtmlVisualizer();

        List<PrioritizedSequenceRearrangement> prioritizedSequenceRearrangements = new ArrayList<>();
        int above = 0, below = 0;
        for (SvannaVariant variant : variants) {
            // run filtering
            FilterResult filterResult = variantFilter.runFilter(variant);
            variant.addFilterResult(filterResult);

            // run prioritization
            SvPriority priority = prioritizer.prioritize(variant);
            priorities.add(priority);
            if (priority.getImpact().satisfiesThreshold(threshold)) {
                prioritizedSequenceRearrangements.add(new PrioritizedSequenceRearrangement(variant, priority));
            } else {
                below++;
            }
        }
        LOGGER.info(" Above threshold SVs: {}, below threshold SVs: {}", NF.format(above), NF.format(below));

        // TODO -- if we have frequency information
        // svList - svann.prioritizeSvsByPopulationFrequency(svList);
        // This filters our SVs with lower impact than our threshold

        FilterAndCount fac = new FilterAndCount(priorities, variants, threshold);
        // Now the list just contains SVs that pass the threshold
       // List<SvPriority> filteredPriorityList = fac.getFilteredPriorityList();

        int unparsableCount = fac.getUnparsableCount();
        Map<VariantType, Integer> lowImpactCounts = fac.getLowImpactCounts();
        Map<VariantType, Integer> intermediateImpactCounts = fac.getIntermediateImpactCounts();
        Map<VariantType, Integer> highImpactCounts = fac.getHighImpactCounts();

        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("vcf_file", vcfFile.toString());
        infoMap.put("unparsable", String.valueOf(unparsableCount));
        infoMap.put("n_affectedGenes", String.valueOf(fac.getnAffectedGenes()));
        infoMap.put("n_affectedEnhancers", String.valueOf(fac.getnAffectedEnhancers()));

        List<String> visualizations = new ArrayList<>();
        Collections.sort(prioritizedSequenceRearrangements);
        for (var pr : prioritizedSequenceRearrangements) {
            Visualizable vizbell = pr.getVisualizable();
            visualizations.add(visualizer.getHtml(vizbell));
        }

        HtmlTemplate template = new HtmlTemplate(visualizations,
                lowImpactCounts,
                intermediateImpactCounts,
                highImpactCounts,
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
    private static class PrioritizedSequenceRearrangement implements Comparable<PrioritizedSequenceRearrangement> {
        private final SvannaVariant structuralVariant;
        private final SvPriority priority;
        private final SvImpact impact;
        /** leftmost chromosome */
        private final Contig contig;
        /** leftmost (5') position */
        private final int position;


        private PrioritizedSequenceRearrangement(SvannaVariant sv, SvPriority priority) {
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
            PrioritizedSequenceRearrangement that = (PrioritizedSequenceRearrangement) o;
            return Objects.equals(structuralVariant, that.structuralVariant) &&
                    Objects.equals(priority, that.priority);
        }
        @Override
        public int hashCode() {
            return Objects.hash(structuralVariant, priority);
        }

        @Override
        public int compareTo(PrioritizedSequenceRearrangement that) {
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
