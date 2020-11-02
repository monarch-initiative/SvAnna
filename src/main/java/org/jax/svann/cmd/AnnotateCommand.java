package org.jax.svann.cmd;


import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svann.analysis.FilterAndCount;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TSpecParser;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseGeneMap;
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
    }

    private static Map<Integer, IntervalArray<Enhancer>> readEnhancerMap(Path enhancerFile) {
        TSpecParser tparser = new TSpecParser(enhancerFile.toString());
        return tparser.getChromosomeToEnhancerIntervalArrayMap();
    }

    /*
     * Key -- gene symbol, value, {@link GeneWithId} object with symbol and id. This map is used to connect
     * geneIDs with gene symbols for display in HTML
     * TODO - this is not very elegant and we may want to refactor.
     */
    private static Map<String, GeneWithId> getGeneSymbolMap() {
        HpoDiseaseGeneMap hpomap = HpoDiseaseGeneMap.loadGenesAndDiseaseMap();
        return hpomap.getGeneSymbolMap();
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
        // enhancers
        Map<Integer, IntervalArray<Enhancer>> enhancerMap = readEnhancerMap(enhancerFile);
        // gene symbols
        Map<String, GeneWithId> geneSymbolMap = getGeneSymbolMap();
        // jannovar data
        JannovarData jannovarData = readJannovarData(jannovarPath);

        // 1 - parse input variants
        BreakendAssembler breakendAssembler = new BreakendAssembler();
        StructuralRearrangementParser parser = new VcfStructuralRearrangementParser(assembly, breakendAssembler);
        Set<TermId> patientTerms = Arrays.stream(hpoTermIdList.split(",")).map(String::trim).map(TermId::of).collect(Collectors.toSet());

        Collection<SequenceRearrangement> rearrangements = parser.parseFile(vcfFile);

        // 2 - prioritize variants
        Overlapper overlapper = new Overlapper(jannovarData);
        EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(jannovarData, enhancerMap);
        SvPrioritizer prioritizer = new PrototypeSvPrioritizer(geneSymbolMap, overlapper, enhancerOverlapper, patientTerms); // how to prioritize
        List<SvPriority> priorities = new ArrayList<>(); // where to store the prioritization results
        for (SequenceRearrangement rearrangement : rearrangements) {
            SvPriority priority = prioritizer.prioritize(rearrangement);
            priorities.add(priority);
        }

        // TODO -- if we have frequency information
        // svList - svann.prioritizeSvsByPopulationFrequency(svList);
        // This filters our SVs with lower impact than our threshold

        // 3 - visualize the results
        // TODO: 2. 11. 2020 this will work once visualizer is refactored,
        //  we can even move this into the loop above
        Visualizer visualizer = new HtmlVisualizer(null);

//        List<Visualizable> visualableList = svList.stream().map(HtmlVisualizable::new).collect(Collectors.toList());
//        List<Visualizer> visualizerList = visualableList.stream().map(HtmlVisualizer::new).collect(Collectors.toList());

        FilterAndCount fac = new FilterAndCount(priorities, threshold);
        List<SvPriority> filteredPriorityList = fac.getFilteredPriorityList();// Now the list just contains SVs that pass the threshold
        int unparsableCount = fac.getUnparsableCount();
        Map<SvType, Integer> lowImpactCounts = fac.getLowImpactCounts();
        Map<SvType, Integer> intermediateImpactCounts = fac.getIntermediateImpactCounts();
        Map<SvType, Integer> highImpactCounts = fac.getHighImpactCounts();

        Map<String, String> infoMap = new HashMap<>();
        infoMap.put("vcf_file", vcfFile.toString());
        infoMap.put("unparsable", String.valueOf(unparsableCount));
//        HtmlTemplate template = new HtmlTemplate(visualizerList,
//                lowImpactCounts,
//                intermediateImpactCounts,
//                highImpactCounts,
//                infoMap);
//        template.outputFile(this.outprefix);

        // We're done!
        return 0;
    }

}
