package org.monarchinitiative.svanna.cli.cmd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.monarchinitiative.svanna.cli.writer.ResultWriterFactory;
import org.monarchinitiative.svanna.configuration.SvAnnaBuilder;
import org.monarchinitiative.svanna.configuration.exception.InvalidResourceException;
import org.monarchinitiative.svanna.configuration.exception.MissingResourceException;
import org.monarchinitiative.svanna.configuration.exception.UndefinedResourceException;
import org.monarchinitiative.svanna.core.SvAnna;
import org.monarchinitiative.svanna.core.configuration.DataProperties;
import org.monarchinitiative.svanna.core.configuration.PrioritizationProperties;
import org.monarchinitiative.svanna.core.configuration.SvAnnaProperties;
import org.monarchinitiative.svanna.core.hpo.IcMicaMode;
import org.monarchinitiative.svanna.core.hpo.TermSimilarityMeasure;
import org.monarchinitiative.svanna.core.overlap.GeneOverlapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Callable;

public abstract class SvAnnaCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SvAnnaCommand.class);

    private static final Properties PROPERTIES = readProperties();
    private static final String SVANNA_VERSION = PROPERTIES.getProperty("svanna.version", "unknown version");
    // The following constants stay here until we find a value of making them external. --------------------------------
    private static final double GENE_FACTOR = 1.;
    private static final double ENHANCER_FACTOR = 1.;
    private static final boolean USE_VISTA = true;
    private static final boolean USE_FANTOM_5 = false;
    private static final double FANTOM_5_TISSUE_SPECIFICITY = .5;
    private static final double TAD_STABILITY_THRESHOLD = 80.;
    // -----------------------------------------------------------------------------------------------------------------

    @CommandLine.Option(names = {"-v"},
            description = {"Specify multiple -v options to increase verbosity.",
                    "For example, `-v -v -v` or `-vvv`"})
    protected boolean[] verbosity = {};

    @CommandLine.Option(names = {"-d", "--data-directory"},
            paramLabel = "path/to/datadir",
            required = true,
            description = "Path to SvAnna data directory.")
    protected Path svannaDataDirectory;

    @CommandLine.ArgGroup(validate = false, heading = "SvAnna configuration:%n")
    protected ConfigurationSection configurationSection = new ConfigurationSection();
    protected static class ConfigurationSection {

        @CommandLine.Option(names = {"--term-similarity-measure"},
                paramLabel = "{RESNIK_SYMMETRIC, RESNIK_ASYMETRIC}",
                description = "Phenotype term similarity measure (default: ${DEFAULT-VALUE}).")
        protected TermSimilarityMeasure termSimilarityMeasure = TermSimilarityMeasure.RESNIK_SYMMETRIC;

        @CommandLine.Option(names = {"--ic-mica-mode"},
                paramLabel = "{DATABASE,IN_MEMORY}",
                description = "The mode for getting information content of the most informative common ancestors for terms t1, and t2 (default: ${DEFAULT-VALUE}).")
        protected IcMicaMode icMicaMode = IcMicaMode.DATABASE;

        @CommandLine.Option(names = {"--promoter-length"},
                description = "Number of bases prepended to a transcript and evaluated as a promoter region (default: ${DEFAULT-VALUE}).")
        protected int promoterLength = 2_000;
        @CommandLine.Option(names = {"--promoter-fitness-gain"},
                description = "Set to 0. to score the promoter variants as strictly as coding variants, or to 1. to skip them altogether (default: ${DEFAULT-VALUE}).")
        protected double promoterFitnessGain = .6;
    }

    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = SvAnnaCommand.class.getResourceAsStream("/svanna.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
        }
        return properties;
    }

    @Override
    public Integer call() {
        // (0) Setup verbosity and print banner.
        setupLoggingAndPrintBanner();

        // (1) Run the command functionality.
        return execute();
    }

    protected abstract Integer execute();

    protected SvAnna bootstrapSvAnna(SvAnnaProperties svAnnaProperties) throws MissingResourceException, InvalidResourceException, UndefinedResourceException {
        LOGGER.info("Spooling up SvAnna v{} using resources in {}", SVANNA_VERSION, svAnnaProperties.dataDirectory().toAbsolutePath());
        return SvAnnaBuilder.builder(svAnnaProperties)
                .build();
    }

    protected PrioritizationProperties prioritizationProperties() {
        return PrioritizationProperties.of(configurationSection.termSimilarityMeasure,
                configurationSection.icMicaMode,
                configurationSection.promoterLength,
                configurationSection.promoterFitnessGain,
                GENE_FACTOR,
                ENHANCER_FACTOR);
    }

    protected DataProperties dataProperties() {
        return DataProperties.of(USE_VISTA,
                USE_FANTOM_5,
                FANTOM_5_TISSUE_SPECIFICITY,
                TAD_STABILITY_THRESHOLD);
    }

    protected static ResultWriterFactory resultWriterFactory(SvAnna svAnna) {
        GeneOverlapper overlapper = GeneOverlapper.of(svAnna.geneService());
        return new ResultWriterFactory(overlapper, svAnna.annotationDataService(), svAnna.phenotypeDataService());
    }

    private void setupLoggingAndPrintBanner() {
        Level level = parseVerbosityLevel();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(level);

        printBanner();
    }

    private static String readBanner() {
        try (InputStream is = new BufferedInputStream(Objects.requireNonNull(SvAnnaCommand.class.getResourceAsStream("/banner.txt")))) {
            return new String(is.readAllBytes());
        } catch (IOException e) {
            // swallow
            return "";
        }
    }

    private Level parseVerbosityLevel() {
        int verbosity = 0;
        for (boolean a : this.verbosity) {
            if (a) verbosity++;
        }

        switch (verbosity) {
            case 0:
                return Level.INFO;
            case 1:
                return Level.DEBUG;
            case 2:
                return Level.TRACE;
            default:
                return Level.ALL;
        }
    }

    private static void printBanner() {
        System.err.println(readBanner());
    }

}
