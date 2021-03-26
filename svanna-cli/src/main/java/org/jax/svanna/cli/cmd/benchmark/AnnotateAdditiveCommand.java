package org.jax.svanna.cli.cmd.benchmark;


import org.jax.svanna.cli.Main;
import org.jax.svanna.core.exception.LogUtils;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@CommandLine.Command(name = "annotate-additive",
        aliases = {"AAV"},
        header = "Prioritize the variants with additive prioritizer",
        mixinStandardHelpOptions = true,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class AnnotateAdditiveCommand extends BaseAdditiveCommand {


    private static final Logger LOGGER = LoggerFactory.getLogger(AnnotateAdditiveCommand.class);

    /*
     * ------------ ANALYSIS OPTIONS ------------
     */
    @CommandLine.Option(names = {"--vcf"}, description = "Path to input VCF file")
    public Path vcfFile = null;

    @CommandLine.Option(names = {"-t", "--term"}, description = "HPO term IDs (comma-separated list)")
    public List<String> hpoTermIdList = List.of();

    @Override
    public Integer call() {
        if (vcfFile == null) {
            LogUtils.logWarn(LOGGER,"Path to a VCF file must be supplied");
            return 1;
        }

        int status = checkArguments();
        if (status!=0)
            return status;

        Set<TermId> phenotypeTermIds = hpoTermIdList.stream()
                .map(TermId::of)
                .collect(Collectors.toSet());

        try {
            runAnalysis(phenotypeTermIds, vcfFile);
        } catch (InterruptedException | ExecutionException | IOException e) {
            LogUtils.logError(LOGGER, "Error: {}", e.getMessage());
            return 1;
        }

        LogUtils.logInfo(LOGGER, "The analysis is complete. Bye");
        return 0;
    }

}
