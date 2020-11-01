package org.jax.svann.cmd;


import org.jax.svann.analysis.FilterAndCount;
import org.jax.svann.analysis.SvAnnAnalysis;
import org.jax.svann.html.HtmlTemplate;
import org.jax.svann.priority.SvImpact;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.SvType;
import org.jax.svann.viz.HtmlVisualizable;
import org.jax.svann.viz.HtmlVisualizer;
import org.jax.svann.viz.Visualizable;
import org.jax.svann.viz.Visualizer;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "annotate", aliases = {"A"}, mixinStandardHelpOptions = true, description = "annotate VCF file")
public class AnnotateCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateCommand.class);

    /**
     * This is what we use, candidate for externalization into a CLI parameter
     */
    private static final String ASSEMBLY_ID = "GRCh38.p13";

    @CommandLine.Option(names = {"-v", "--vcf"}, required = true)
    protected String vcfFile;
    @CommandLine.Option(names = {"-e","--enhancer"},  description = "tspec enhancer file")
    private String enhancerFile = null;
    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list)")
    private String hpoTermIdList = null;
    @CommandLine.Option(names = {"-g", "--gencode"})
    private String geneCodePath = "data/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz";
    @CommandLine.Option(names={"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    private String outprefix = "SVANN";
    @CommandLine.Option(names={"-j", "--jannovar"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    private String jannovarPath = "data/data/hg38_refseq_curated.ser";

    public AnnotateCommand() {
    }

    @Override
    public Integer call() throws Exception {
        SvAnnAnalysis svann;
        List<TermId> tidList;
        if (hpoTermIdList != null) {
            String[] ids = hpoTermIdList.split(",");
            tidList = Arrays.stream(ids).map(TermId::of).collect(Collectors.toList());
        } else {
            tidList = List.of(); // HPO terms are optional, we can pass an empty list
            // and this will turn off HPO prioritization.
        }
        svann = new SvAnnAnalysis(this.vcfFile, this.outprefix, this.enhancerFile, this.jannovarPath, tidList);
        List<SvPriority> svList = svann.getRawSequenceRearrangments();
        svList = svann.prioritizeSvsBySequence(svList);
        if (tidList.size() > 0) {
            svList = svann.prioritizeSvsByPhenotype(svList);
        }
        // TODO -- if we have frequency information
        // svList - svann.prioritizeSvsByPopulationFrequency(svList);
        // This filters our SVs with lower impact than our threshold
        SvImpact threshold = SvImpact.HIGH_IMPACT;  // TODO  -- set on command line
        FilterAndCount fac = new FilterAndCount(svList, threshold);
        svList = fac.getFilteredPriorityList(); // Now the list just contains SVs that pass the threshold
        Map<SvType, Integer> lowImpactCounts = fac.getLowImpactCounts();
        Map<SvType, Integer> intermediateImpactCounts = fac.getIntermediateImpactCounts();
        Map<SvType, Integer> highImpactCounts = fac.getHighImpactCounts();
        int unparsable = fac.getUnparsableCount();

        List<Visualizable> visualableList = svList.stream().map(HtmlVisualizable::new).collect(Collectors.toList());
        List<Visualizer> visualizerList = visualableList.stream().map(HtmlVisualizer::new).collect(Collectors.toList());
        Map<String,String> infoMap = new HashMap<>();
        infoMap.put("vcf_file", vcfFile);
        infoMap.put("unparsable", String.valueOf(unparsable));
        HtmlTemplate template = new HtmlTemplate(visualizerList,
                lowImpactCounts,
                intermediateImpactCounts,
                highImpactCounts,
                infoMap);
        template.outputFile(this.outprefix);
        return 0;
    }

}
