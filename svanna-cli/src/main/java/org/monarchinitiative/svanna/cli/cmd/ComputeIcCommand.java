package org.monarchinitiative.svanna.cli.cmd;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.monarchinitiative.svanna.cli.Main;
import org.monarchinitiative.svanna.core.ic.PrecomputeIcMica;
import org.monarchinitiative.svanna.io.IOUtils;
import org.monarchinitiative.svanna.io.hpo.IcMicaDictUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.GZIPOutputStream;

@CommandLine.Command(name = "compute-ic",
        header = "Precompute IC MICA values for HPO term pairs.",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class ComputeIcCommand extends BaseSvAnnaCommand {

    protected static final NumberFormat NF = NumberFormat.getNumberInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputeIcCommand.class);

    static {
        NF.setMaximumFractionDigits(2);
    }

    @CommandLine.Option(names = {"--output"},
            description = "Where to write the term pair similarity table (default: ${DEFAULT-VALUE})")
    public Path output = Path.of(IcMicaDictUtils.TERM_PAIR_SIMILARITY_NAME);
    @CommandLine.Option(names = {"--hpo"},
            paramLabel = "path/to/hp.json",
            required = true,
            description = "Path to HPO file.")
    protected Path hpJsonPath;
    @CommandLine.Option(names = {"--hpoa"},
            paramLabel = "path/to/phenotype.hpoa",
            required = true,
            description = "Path to HPO annotations file.")
    protected Path hpAnnotationPath;


    @Override
    protected Integer execute() {
        MinimalOntology hpo = MinimalOntologyLoader.loadOntology(hpJsonPath.toFile());
        HpoDiseaseLoader hpoaLoader = HpoDiseaseLoaders.defaultLoader(hpo, HpoDiseaseLoaderOptions.defaultOptions());
        try {
            HpoDiseases diseases = hpoaLoader.load(hpAnnotationPath);
            Map<TermPair, Double> icMicaMap = PrecomputeIcMica.precomputeIcMicaMap(hpo, diseases, true);

            LocalDate date = LocalDate.now();
            String hpoVersion = hpo.version().orElse("N/A");
            String hpoaVersion = diseases.version().orElse("N/A");
            try (Writer writer = IOUtils.openForWriting(output)) {
                IcMicaDictUtils.writeTermPairMap(icMicaMap, writer, date, hpoVersion, hpoaVersion);
            }
        } catch (IOException e) {
            LOGGER.error("Error: {}", e.getMessage());
            LOGGER.debug("Error: {}", e.getMessage(), e);
            return 1;
        }

        return 0;
    }
}
