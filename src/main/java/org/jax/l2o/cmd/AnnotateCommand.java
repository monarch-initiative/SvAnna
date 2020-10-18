package org.jax.l2o.cmd;

import org.jax.l2o.Lirical2Overlap;
import org.jax.l2o.html.HtmlTemplate;
import org.jax.l2o.lirical.LiricalHit;
import org.jax.l2o.vcf.SvAnnOld;
import org.jax.l2o.vcf.VcfSvParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list) to classify enhancers")
    private String hpoTermId = null;
    @CommandLine.Option(names = {"-g", "--gencode"})
    private String geneCodePath = "data/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz";
    @CommandLine.Option(names = {"--igv"}, description = "output position file for use with IGV")
    private boolean outputIgvFile = false;
    @CommandLine.Option(names={"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    private String outprefix = "L2O";
    @CommandLine.Option(names={"-j", "--jannovar"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    private String jannovarPath = "data/data/hg38_refseq_curated.ser";

    public AnnotateCommand() {
    }

    @Override
    public Integer call() {
        Lirical2Overlap l2o;
        if (enhancerFile != null && hpoTermId != null) {
            String [] ids = hpoTermId.split(",");
            List<TermId> tidList = Arrays.stream(ids).map(TermId::of).collect(Collectors.toList());
            l2o = new Lirical2Overlap(this.liricalFile, this.vcfFile, this.outname, tidList, this.enhancerFile, this.geneCodePath);
        } else {
            l2o = new Lirical2Overlap(this.liricalFile, this.vcfFile, this.outname);
        }
        List<LiricalHit> hitlist = l2o.getHitlist();
        VcfSvParser vcfParser = new VcfSvParser(this.vcfFile, this.jannovarPath);
        List<SvAnnOld> annList = List.of();//vcfParser.getAnnlist();
        List<SvAnnOld> translocationList = List.of();//vcfParser.getTranslocationList();
        HtmlTemplate template = new HtmlTemplate(annList, translocationList);
        template.outputFile(this.outprefix);
        if (this.outputIgvFile) {
            outputIgvTargetsFile(annList);
        }
        return 0;
    }

    /**
     * Output the chromosomal locations of the "interesting" SVs to a file that can be used to find the SVs in IGV.
     * @param svlist
     */
    private void outputIgvTargetsFile(List<SvAnnOld> svlist) {
        String fname = String.format("%s.igv.txt", this.outprefix);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fname))) {
            for (SvAnnOld sv : svlist) {
                if (sv.isLowPriority()) {
                    continue;
                }
                //String bedline = sv.getBedLine();
                String igvline = sv.getIgvLine();
                writer.write(igvline + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
