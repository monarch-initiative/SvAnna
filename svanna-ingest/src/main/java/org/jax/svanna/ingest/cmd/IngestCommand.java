package org.jax.svanna.ingest.cmd;

import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.EnhancerTissueSpecificity;
import org.jax.svanna.ingest.Main;
import org.jax.svanna.ingest.hpomap.HpoMapping;
import org.jax.svanna.ingest.hpomap.HpoTissueMapParser;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.jax.svanna.ingest.parse.enhancer.fantom.FantomEnhancerParser;
import org.jax.svanna.ingest.parse.enhancer.vista.VistaEnhancerParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicAssemblies;
import org.monarchinitiative.svart.GenomicAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;


@CommandLine.Command(name = "ingest",
        aliases = "I",
        header = "intest and transform annotation files",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
@Deprecated
public class IngestCommand implements Callable<Integer> {
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestCommand.class);
    @CommandLine.Option(names = {"-f", "--fantom"},
                        description = "path to F5.hg38.enhancers.expression.matrix.gz file",
                        required = true)
    public Path f5Hg38EnhancerPath;
    @CommandLine.Option(names={"-s", "--sample"},
                        description = "path to Human.sample_name2library_id.txt file",
                        required = true)
    public Path samplePath;
    @CommandLine.Option(names = {"-v", "--vista"},
                        description = "path to vista-hg38.bed file",
                        required = true)
    public Path vistaPath;
    /**
     * Threshold to discard enhancers below this percentile threshold of tau or CPM expression.
     */
    @CommandLine.Option(names = {"-t","--threshold"}, description = "default percentile threshold for enhancers")
    public int percentileThreshold = 20;
    @CommandLine.Option(names = {"-o", "--outfile"}, description = "name of outfile (default: ${DEFAULT-VALUE})")
    public Path outfile = Paths.get("hg38Enhancers.tsv");

    @Override
    public Integer call() {
        try {
            GenomicAssembly assembly = GenomicAssemblies.GRCh38p13();
            File tissueMap = getResource("hpo_enhancer_map.csv");
            HpoTissueMapParser hpoTissueMapParser = new HpoTissueMapParser(tissueMap);
            Map<TermId, HpoMapping> uberonToHpoMap = hpoTissueMapParser.getOtherToHpoMap();
//        VistaParser vistaParser = new VistaParser(this.vistaPath, uberonToHpoMap);
//        List<IngestedEnhancer> vistaEnhancers = vistaParser.getEnhancers();
            IngestRecordParser<? extends Enhancer> vistaParser = new VistaEnhancerParser(assembly, vistaPath, uberonToHpoMap);
            List<? extends Enhancer> vistaEnhancers = vistaParser.parseToList();
//        Fantom5Parser fantom5Parser = new Fantom5Parser(this.f5Hg38EnhancerPath, this.samplePath, this.percentileThreshold);
            IngestRecordParser<? extends Enhancer> fantomParser = new FantomEnhancerParser(assembly, f5Hg38EnhancerPath, samplePath, uberonToHpoMap);
            List<? extends Enhancer> fantomEnhancers = fantomParser.parseToList();
//        double thresholdCpm = fantom5Parser.getCpmThreshold();
//        double tauAtThreshold = fantom5Parser.getTauThreshold();
//        System.out.printf("[INFO] CPM at %d percentile: %f\n", this.percentileThreshold, thresholdCpm);
//        System.out.printf("[INFO] Tau at %d percentile: %f\n", this.percentileThreshold, tauAtThreshold);
//        List<IngestedEnhancer> aboveThresholdEnhancers = fantom5Parser.getAboveThresholdEnhancers(thresholdCpm, tauAtThreshold);
//        System.out.printf("[INFO] We extracted %d VISTA and %d FANTOM5 enhancers\n", vistaEnhancers.size(), aboveThresholdEnhancers.size());
            if (LOGGER.isInfoEnabled())
                LOGGER.info("Storing the enhancers to `{}`", outfile.toAbsolutePath());
            try (BufferedWriter bw = Files.newBufferedWriter(outfile)) {
                bw.write("name\tchrom\tbegin\tend\ttissues\n");
                for (var enhancer : vistaEnhancers) {
                    String tissues = enhancer.tissueSpecificity().stream().map(toTissueSpecificitySummary()).collect(Collectors.joining(";"));
                    String line = String.format("%s\t%s\t%d\t%d\t%s\n", enhancer.id(), enhancer.contigName(), enhancer.startWithCoordinateSystem(CoordinateSystem.zeroBased()), enhancer.endWithCoordinateSystem(CoordinateSystem.zeroBased()), tissues);
                    bw.write(line);
                }
                for (var ie : fantomEnhancers) {
                    String tissues = ie.tissueSpecificity().stream().map(toTissueSpecificitySummary()).collect(Collectors.joining(";"));
                    String line = String.format("%s\t%s\t%d\t%d\t%s\n", ie.id(), ie.contigName(), ie.startWithCoordinateSystem(CoordinateSystem.zeroBased()), ie.endWithCoordinateSystem(CoordinateSystem.zeroBased()), tissues);
                    bw.write(line);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error: {}", e.getMessage());
            return 1;
        }
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

    private static Function<EnhancerTissueSpecificity, String> toTissueSpecificitySummary() {
        return tissue -> String.format("%s{%s}%s{%s}",
                tissue.tissueTerm().getId().getValue(), tissue.tissueTerm().getName(), tissue.hpoTerm().getId().getValue(), tissue.hpoTerm().getName());
    }

}
