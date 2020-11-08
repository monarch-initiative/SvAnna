package org.jax.svann.cmd;


import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.apache.logging.log4j.core.net.Priority;
import org.jax.svann.analysis.FilterAndCount;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TSpecParser;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseGeneMap;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.html.HtmlTemplate;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.parse.BreakendAssembler;
import org.jax.svann.parse.StructuralRearrangementParser;
import org.jax.svann.parse.VcfStructuralRearrangementParser;
import org.jax.svann.priority.PrototypeSvPrioritizer;
import org.jax.svann.priority.SvImpact;
import org.jax.svann.priority.SvPrioritizer;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.jax.svann.viz.HtmlVisualizable;
import org.jax.svann.viz.HtmlVisualizer;
import org.jax.svann.viz.Visualizer;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "annotate", aliases = {"A"}, mixinStandardHelpOptions = true, description = "annotate VCF file")
public class AnnotateCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateCommand.class);

    /**
     * This is what we use, candidate for externalization into a CLI parameter
     */
    private static final String ASSEMBLY_ID = "GRCh38.p13";
    private final HpoDiseaseGeneMap hpoDiseaseGeneMap;
    @CommandLine.Option(names = {"-j", "--jannovar"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    public Path jannovarPath = Paths.get("data/data/hg38_refseq_curated.ser");
    @CommandLine.Option(names = {"-g", "--gencode"})
    public Path geneCodePath = Paths.get("data/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz");
    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    public String outprefix = "SVANN";
    @CommandLine.Option(names = {"-v", "--vcf"}, required = true)
    public Path vcfFile;
    @CommandLine.Option(names = {"-e", "--enhancer"}, description = "tspec enhancer file")
    public Path enhancerFile;
    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list)")
    public String hpoTermIdList;
    @CommandLine.Option(names = {"--threshold"},
            type = SvImpact.class,
            description = "report variants as severe as this or more")
    public SvImpact threshold = SvImpact.LOW;

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
        Optional<GenomeAssembly> assemblyOptional = GenomeAssemblyProvider.getDefaultProvider().getAssembly(ASSEMBLY_ID);
        if (assemblyOptional.isEmpty()) {
            // TODO(DD): 2. 11. 2020 this needs to be improved
            LOGGER.warn("Assembly {} not available", ASSEMBLY_ID);
            return 1;
        }
        GenomeAssembly assembly = assemblyOptional.get();
        // patient phenotype
        Set<TermId> patientTerms = Arrays.stream(hpoTermIdList.split(",")).map(String::trim).map(TermId::of).collect(Collectors.toSet());
        // check that the HPO terms entered by the user (if any) are valid
        Map<TermId, String> hpoTermsAndLabels;
        if (! patientTerms.isEmpty())
            hpoTermsAndLabels = hpoDiseaseGeneMap.getTermLabelMap(patientTerms);
        else
            hpoTermsAndLabels = Map.of();
        // enhancers & relevant enhancer terms
        TSpecParser tparser = new TSpecParser(enhancerFile.toString());
        Map<Integer, IntervalArray<Enhancer>> enhancerMap = tparser.getChromosomeToEnhancerIntervalArrayMap();
        Set<TermId> enhancerRelevantAncestors = hpoDiseaseGeneMap.getRelevantAncestors(tparser.getId2enhancerMap().keySet(), patientTerms);
        // gene symbols
        Map<String, GeneWithId> geneSymbolMap = hpoDiseaseGeneMap.getGeneSymbolMap();
        // jannovar data
        JannovarData jannovarData = readJannovarData(jannovarPath);
        // disease summary map
        // TODO: 4. 11. 2020 implement
        Map<TermId, Set<HpoDiseaseSummary>> diseaseSummaryMap = Map.of();

        // 1 - parse input variants
        BreakendAssembler breakendAssembler = new BreakendAssembler();
        StructuralRearrangementParser parser = new VcfStructuralRearrangementParser(assembly, breakendAssembler);

        List<SequenceRearrangement> rearrangements = parser.parseFile(vcfFile);

        // 2 - prioritize & visualize variants
        // setup prioritization parts
        Overlapper overlapper = new Overlapper(jannovarData);
        EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(jannovarData, enhancerMap);

        SvPrioritizer prioritizer = new PrototypeSvPrioritizer(overlapper, enhancerOverlapper, geneSymbolMap, patientTerms, enhancerRelevantAncestors, diseaseSummaryMap);
        List<SvPriority> priorities = new ArrayList<>(); // where to store the prioritization results
        // setup visualization parts
        Visualizer visualizer = new HtmlVisualizer();
        List<String> visualizations = new ArrayList<>();
        int above=0,below=0;
        for (SequenceRearrangement rearrangement : rearrangements) {
            SvPriority priority = prioritizer.prioritize(rearrangement);
            priorities.add(priority);
            if (priority.getImpact().satisfiesThreshold(threshold)) {
                HtmlVisualizable visualizable = new HtmlVisualizable(rearrangement, priority);
                String visualization = visualizer.getHtml(visualizable);
                above++; if (above>100) continue;
               visualizations.add(visualization);

            } else {
                below++;
            }
        }
        System.out.printf("[INFO] Above threshold SVs: %d, below threshold SVs: %d.\n", above, below);

        // TODO -- if we have frequency information
        // svList - svann.prioritizeSvsByPopulationFrequency(svList);
        // This filters our SVs with lower impact than our threshold

        FilterAndCount fac = new FilterAndCount(priorities, rearrangements, threshold);
        // Now the list just contains SVs that pass the threshold
        List<SvPriority> filteredPriorityList = fac.getFilteredPriorityList();
        int unparsableCount = fac.getUnparsableCount();
        Map<SvType, Integer> lowImpactCounts = fac.getLowImpactCounts();
        Map<SvType, Integer> intermediateImpactCounts = fac.getIntermediateImpactCounts();
        Map<SvType, Integer> highImpactCounts = fac.getHighImpactCounts();

        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("vcf_file", vcfFile.toString());
        infoMap.put("unparsable", String.valueOf(unparsableCount));

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

}
