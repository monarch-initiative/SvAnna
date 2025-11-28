package org.monarchinitiative.svanna.cli.cmd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.Callable;

abstract class BaseSvAnnaCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSvAnnaCommand.class);

    @CommandLine.Option(names = {"-v"}, description = {"Specify multiple -v options to increase verbosity.", "For example, `-v -v -v` or `-vvv`"})
    protected boolean[] verbosity = {};

    @Override
    public Integer call() {
        // (0) Setup verbosity and print banner.
        setupLoggingAndPrintBanner();

        // (1) Run the command functionality.
        Integer status = execute();
        if (status == 0)
            LOGGER.info("We're done here, bye!");
        return status;
    }

    protected abstract Integer execute();

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
