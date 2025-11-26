package org.monarchinitiative.svanna.cli.cmd;

import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.monarchinitiative.svanna.cli.Main;
import org.monarchinitiative.svanna.configuration.SvannaDataResolver;
import org.monarchinitiative.svanna.configuration.exception.MissingResourceException;
import org.monarchinitiative.svanna.core.ic.PrecomputeIcMica;
import org.monarchinitiative.svanna.io.IOUtils;
import org.monarchinitiative.svanna.io.hpo.IcMicaDictUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Map;

@CommandLine.Command(name = "setup-phenotype",
        header = "Setup gene-phenotype resources.",
        mixinStandardHelpOptions = true,
        sortOptions = false,
        version = Main.VERSION,
        usageHelpWidth = Main.WIDTH,
        footer = Main.FOOTER)
public class SetupPhenotypeCommand extends BaseSvAnnaCommand {

    protected static final NumberFormat NF = NumberFormat.getNumberInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(SetupPhenotypeCommand.class);

    static {
        NF.setMaximumFractionDigits(2);
    }

    @CommandLine.Option(names = {"-d", "--data-directory"},
            paramLabel = "path/to/datadir",
            required = true,
            description = "Path to SvAnna data directory.")
    protected Path svannaDataDirectory;

    @CommandLine.Option(
            names = {"-w", "--overwrite"},
            description = "Force overwrite"
    )
    protected boolean overwrite = false;

    @Override
    protected Integer execute() {
        SvannaDataResolver resolver;
        try {
            resolver = new SvannaDataResolver(svannaDataDirectory, false);
            LOGGER.info("Setting up phenotype at {}", resolver.phenotypeDataDirectory().toAbsolutePath());
            if (overwrite)
                LOGGER.info("Overwriting existing files");
            downloadFiles(resolver);

            if (!resolver.termToIcMicaPath().toFile().isFile() || overwrite) {
                computeTermPairToIcMicaTable(resolver);
            }
        } catch (IOException | FileDownloadException | MissingResourceException e) {
            LOGGER.error("Error: {}", e.getMessage());
            LOGGER.debug("Error: {}", e.getMessage(), e);
            return 1;
        }

        return 0;
    }

    private static void computeTermPairToIcMicaTable(SvannaDataResolver resolver) throws IOException {
        MinimalOntology hpo = MinimalOntologyLoader.loadOntology(resolver.hpOntologyPath().toFile());
        HpoDiseaseLoader hpoaLoader = HpoDiseaseLoaders.defaultLoader(hpo, HpoDiseaseLoaderOptions.defaultOptions());
        HpoDiseases diseases = hpoaLoader.load(resolver.phenotypeHpoaPath());
        Map<TermPair, Double> icMicaMap = PrecomputeIcMica.precomputeIcMicaMap(hpo, diseases, true);

        LocalDate date = LocalDate.now();
        String hpoVersion = hpo.version().orElse("N/A");
        String hpoaVersion = diseases.version().orElse("N/A");
        try (Writer writer = IOUtils.openForWriting(resolver.termToIcMicaPath())) {
            IcMicaDictUtils.writeTermPairMap(icMicaMap, writer, date, hpoVersion, hpoaVersion);
        }
    }

    private void downloadFiles(SvannaDataResolver resolver) throws FileDownloadException {
        BioDownloader downloader = BioDownloader.builder(resolver.phenotypeDataDirectory())
                .overwrite(overwrite)
                .hpoJson()
                .hpDiseaseAnnotations()
                .hgnc()
                .medgene2MIM()
                .build();

        downloader.download();
    }
}
