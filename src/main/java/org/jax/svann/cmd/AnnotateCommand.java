package org.jax.svann.cmd;


import org.jax.svann.SvAnnAnalysis;
import org.jax.svann.html.HtmlTemplate;
import org.jax.svann.reference.IntrachromosomalEvent;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;
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
    @CommandLine.Option(names = {"-l", "--lirical"}, required = true)
    protected String liricalFile;
    @CommandLine.Option(names = {"-e","--enhancer"},  description = "tspec enhancer file")
    private String enhancerFile = null;
    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list)")
    private String hpoTermIdList = null;
    @CommandLine.Option(names = {"-g", "--gencode"})
    private String geneCodePath = "data/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz";
    @CommandLine.Option(names={"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    private String outprefix = "L2O";
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
        // 1. Prioritize the sequences by overlap with
        svann.prioritizeSymbolSvs();
        svann.prioritizeBreakendSvs();
        // 2. prioritize by phenotype
        if (tidList.size()>0) {
            svann.prioritizeByPhenotype();
        }

        List<IntrachromosomalEvent> annList = List.of();//vcfParser.getAnnlist();
        HtmlTemplate template = new HtmlTemplate(annList);
        template.outputFile(this.outprefix);
        return 0;
    }

}
