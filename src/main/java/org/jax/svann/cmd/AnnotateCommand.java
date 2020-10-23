package org.jax.svann.cmd;

import org.jax.svann.SvAnnotator;
import org.jax.svann.html.HtmlTemplate;
import org.jax.svann.lirical.LiricalHit;
import org.jax.svann.vcf.SvAnnOld;
import org.jax.svann.vcf.VcfSvParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "annotate", aliases = {"A"}, mixinStandardHelpOptions = true, description = "annotate VCF file")
public class AnnotateCommand implements Callable<Integer> {
    @CommandLine.Option(names = {"-o","--out"})
    protected String outname = "l2o.bed";
    @CommandLine.Option(names = {"-v", "--vcf"}, required = true)
    protected String vcfFile;
    @CommandLine.Option(names = {"-l", "--lirical"}, required = true)
    protected String liricalFile;
    @CommandLine.Option(names = {"-e","--enhancer"},  description = "tspec enhancer file")
    private String enhancerFile = null;
    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list)")
    private String hpoTermId = null;
    @CommandLine.Option(names = {"-g", "--gencode"})
    private String geneCodePath = "data/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz";
    @CommandLine.Option(names={"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    private String outprefix = "L2O";
    @CommandLine.Option(names={"-j", "--jannovar"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    private String jannovarPath = "data/data/hg38_refseq_curated.ser";

    public AnnotateCommand() {
    }

    @Override
    public Integer call() {
        SvAnnotator l2o;
        if (enhancerFile != null && hpoTermId != null) {
            String [] ids = hpoTermId.split(",");
            List<TermId> tidList = Arrays.stream(ids).map(TermId::of).collect(Collectors.toList());
            l2o = new SvAnnotator(this.liricalFile, this.vcfFile, this.outname, tidList, this.enhancerFile, this.geneCodePath);
        } else {
            l2o = new SvAnnotator(this.liricalFile, this.vcfFile, this.outname);
        }
        List<LiricalHit> hitlist = l2o.getHitlist();
        VcfSvParser vcfParser = new VcfSvParser(this.vcfFile, this.jannovarPath);
        List<SvAnnOld> annList = List.of();//vcfParser.getAnnlist();
        HtmlTemplate template = new HtmlTemplate(annList);
        template.outputFile(this.outprefix);
        return 0;
    }


}
