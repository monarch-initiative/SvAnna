package org.jax.svann.cmd;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.SerializationException;
import org.jax.svann.SvAnnAnalysis;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.html.HtmlTemplate;
import org.jax.svann.lirical.LiricalHit;
import org.jax.svann.vcf.SvAnnOld;
import org.jax.svann.vcf.VcfSvParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    @CommandLine.Option(names = {"-o", "--out"})
    protected String outname = "l2o.bed";
    @CommandLine.Option(names = {"-v", "--vcf"}, required = true)
    protected String vcfFile;
    @CommandLine.Option(names = {"-l", "--lirical"}, required = true)
    protected String liricalFile;
    @CommandLine.Option(names = {"-e", "--enhancer"}, description = "tspec enhancer file")
    private final String enhancerFile = null;
    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list) to classify enhancers")
    private final String hpoTermId = null;
    @CommandLine.Option(names = {"-g", "--gencode"})
    private final String geneCodePath = "data/gencode.v35.chr_patch_hapl_scaff.basic.annotation.gtf.gz";
    @CommandLine.Option(names = {"--igv"}, description = "output position file for use with IGV")
    private final boolean outputIgvFile = false;
    @CommandLine.Option(names = {"-x", "--prefix"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    private final String outprefix = "L2O";
    @CommandLine.Option(names = {"-j", "--jannovar"}, description = "prefix for output files (default: ${DEFAULT-VALUE} )")
    private final String jannovarPath = "data/data/hg38_refseq_curated.ser";

    public AnnotateCommand() {
    }

    @Override
    public Integer call() {
        SvAnnAnalysis l2o;
        if (enhancerFile != null && hpoTermId != null) {
            String[] ids = hpoTermId.split(",");
            List<TermId> tidList = Arrays.stream(ids).map(TermId::of).collect(Collectors.toList());
            l2o = new SvAnnAnalysis(this.liricalFile, this.vcfFile, this.outname, tidList, this.enhancerFile, this.geneCodePath);
        } else {
            l2o = new SvAnnAnalysis(this.liricalFile, this.vcfFile, this.outname);
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

    /**
     * Deserialize the Jannovar transcript data file that comes with Exomiser. Note that Exomiser
     * uses its own ProtoBuf serializetion and so we need to use its Deserializser. In case the user
     * provides a standard Jannovar serialzied file, we try the legacy deserializer if the protobuf
     * deserializer doesn't work.
     *
     * @return the object created by deserializing a Jannovar file.
     */
    private JannovarData readJannovarData(String jannovarPath) throws SerializationException {
        File f = new File(jannovarPath);
        if (!f.exists()) {
            throw new SvAnnRuntimeException("[FATAL] Could not find Jannovar transcript file at " + jannovarPath);
        }
        try {
            return new JannovarDataSerializer(jannovarPath).load();
        } catch (SerializationException e) {
            LOGGER.error("Could not deserialize Jannovar file with legacy deserializer...");
            throw new SvAnnRuntimeException(String.format("Could not load Jannovar data from %s (%s)",
                    jannovarPath, e.getMessage()));
        }
    }
}
