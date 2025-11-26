package org.monarchinitiative.svanna.ingest.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;

public class IOUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtils.class);

    private IOUtils() {
        // private static class
    }

    /**
     * @return function to close {@link Closeable}s, useful for e.g. closing reader/input stream after stream processing
     * is completed.
     */
    public static Runnable close(Closeable... closeables) {
        return () -> {
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    if (LOGGER.isWarnEnabled())
                        LOGGER.warn("Error closing reader: {}", e.getMessage());
                }
            }
        };
    }

    public static BufferedReader openForReading(Path tablePath) throws IOException {
        return (tablePath.toFile().getName().endsWith(".gz"))
                ? new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(tablePath))))
                : Files.newBufferedReader(tablePath);

    }
}
