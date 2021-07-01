package org.jax.svanna.ingest.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

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
}
