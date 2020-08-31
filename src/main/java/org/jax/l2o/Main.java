package org.jax.l2o;


import org.jax.l2o.html.HtmlTemplate;
import org.jax.l2o.io.HpoDownloader;
import org.jax.l2o.lirical.LiricalHit;
import org.jax.l2o.vcf.AnnotatedVcfParser;
import org.jax.l2o.vcf.SvAnn;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "l2o", mixinStandardHelpOptions = true, version = "l2o 0.0.1",
        description = "LIRICAL to overlapping SV")
public class Main implements Callable<Integer>  {
    @CommandLine.Option(names = {"-o","--out"})
    protected String outname = "l2o.bed";
    @CommandLine.Option(names = {"-v", "--vcf"}, required = true)
    protected String vcfFile;
    @CommandLine.Option(names = {"-l", "--lirical"}, required = true)
    protected String liricalFile;




    public static void main(String [] args) {
        CommandLine cline = new CommandLine(new Main());
        cline.setToggleBooleanFlags(false);
        int exitCode = cline.execute(args);
        System.exit(exitCode);
    }


    private void outputIgvTargetsFile(List<SvAnn> svlist, String fname) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fname))) {
            for (SvAnn sv : svlist) {
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

    @Override
    public Integer call() throws Exception {
        Main main = new Main();

        HpoDownloader downloader = new HpoDownloader("data");
        downloader.download();
        Lirical2Overlap l2o = new Lirical2Overlap(this.liricalFile, this.vcfFile, this.outname);
        List<LiricalHit> hitlist = l2o.getHitlist();
        AnnotatedVcfParser vcfParser = new AnnotatedVcfParser(this.vcfFile, hitlist);
        List<SvAnn> annList = vcfParser.getAnnlist();
        List<SvAnn> translocationList = vcfParser.getTranslocationList();
        HtmlTemplate template = new HtmlTemplate(annList, translocationList);
        template.outputFile();
        outputIgvTargetsFile(annList,"l2o.igv.txt");
        return 0;
    }
}
