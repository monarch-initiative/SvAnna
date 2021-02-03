package org.jax.svanna.cmd;

import org.jax.svanna.Main;
import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.enhancer.IngestedEnhancer;
import org.jax.svanna.enhancer.fantom.Fantom5Parser;
import org.jax.svanna.hpomap.HpoMapping;
import org.jax.svanna.hpomap.HpoTissueMapParser;
import org.jax.svanna.enhancer.vista.VistaParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;



@CommandLine.Command(name = "ingest",
        aliases = "I",
        header = "intest and transform annotation files",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class IngestCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestCommand.class);
    @CommandLine.Option(names = {"-f", "--fantom"},
                        description = "path to F5.hg38.enhancers.expression.matrix.gz file",
                        required = true)
    public String f5Hg38EnhancerPath;
    @CommandLine.Option(names={"-s", "--sample"},
                        description = "path to Human.sample_name2library_id.txt file",
                        required = true)
    public String samplePath;
    @CommandLine.Option(names = {"-v", "--vista"},
                        description = "path to vista-hg38.bed file",
                        required = true)
    public String vistaPath;
    /**
     * Threshold to discard enhancers below this percentile threshold of tau or CPM expression.
     */
    @CommandLine.Option(names = {"-t","--threshold"}, description = "default percentile threshold for enhancers")
    public int percentileThreshold = 20;

    @Override
    public Integer call() {
        File tissueMap = getResource("hpo_enhancer_map.csv");
        HpoTissueMapParser hpoTissueMapParser = new HpoTissueMapParser(tissueMap);
        Map<TermId, HpoMapping> uberonToHpoMap = hpoTissueMapParser.getOtherToHpoMap();
        VistaParser vistaParser = new VistaParser(this.vistaPath, uberonToHpoMap);
        List<IngestedEnhancer> vistaEnhancers = vistaParser.getEnhancers();
        Fantom5Parser fantom5Parser = new Fantom5Parser(this.f5Hg38EnhancerPath, this.samplePath, this.percentileThreshold);
        double thresholdCpm = fantom5Parser.getCpmThreshold();
        double tauAtThreshold = fantom5Parser.getTauThreshold();
        System.out.printf("[INFO] CPM at %d percentile: %f\n", this.percentileThreshold, thresholdCpm);
        System.out.printf("[INFO] Tau at %d percentile: %f\n", this.percentileThreshold, tauAtThreshold);
        List<IngestedEnhancer> fantom5 = fantom5Parser.getAboveThresholdEnhancers(thresholdCpm, tauAtThreshold);
        System.out.printf("[INFO] We extracted %d VISTA and %d FANTOM5 enhancers\n", vistaEnhancers.size(), fantom5.size());
        return 0;
    }

    /**
     * Convenience function for returning path of a file in the src/main/resources directory
     * @param fname name of a file in src/main/resources
     * @return absolute path of the file
     */
    private File getResource(String fname) {
        URL resource = getClass().getClassLoader().getResource(fname);
        if (resource == null) {
            throw new IllegalArgumentException("file not found: " + fname);
        } else {
            try {
                return new File(resource.toURI());
            } catch (URISyntaxException e) {
                throw new SvAnnRuntimeException("Could not retrieve file from resources directory: " + e.getMessage());
            }
        }
    }


}
