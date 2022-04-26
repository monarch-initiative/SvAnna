package org.monarchinitiative.svanna.cli.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Daniel Danis
 */
public class ProgressReporter {

    protected static final NumberFormat NUMBER_FORMAT;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProgressReporter.class);

    static {
        NUMBER_FORMAT = NumberFormat.getNumberInstance();
        NUMBER_FORMAT.setMaximumFractionDigits(2);
    }

    protected final Instant begin;
    protected final AtomicInteger itemCount = new AtomicInteger(0);
    /**
     * We report each n-th instance
     */
    private final int tick;
    private final AtomicReference<Instant> localBegin;

    public ProgressReporter(int tick) {
        this.tick = tick;
        begin = Instant.now();
        localBegin = new AtomicReference<>(begin);
    }

    public <T> void logItem(T item) {
        int current = itemCount.incrementAndGet();
        if (current % tick == 0) {
            Instant end = Instant.now();
            Instant begin = localBegin.getAndSet(end);
            Duration duration = Duration.between(begin, end);
            long ms = duration.toMillis();
            LOGGER.info("Processed {} alleles at {} items/s", NUMBER_FORMAT.format(current), NUMBER_FORMAT.format(((double) tick * 1000) / ms));
        }
    }

    public Runnable summarize() {
        return () -> {
            Duration duration = Duration.between(begin, Instant.now());
            long totalMillis = duration.toMillis();
            double items = itemCount.get();
            double itemsPerSecond = (items * 1000) / totalMillis;
            long mins = (totalMillis / 1000) / 60 % 60;
            long seconds = totalMillis / 1000 % 60;
            LOGGER.info("Processed {} alleles in {}m {}s ({} totalMillis) at {} items/s",
                    NUMBER_FORMAT.format(itemCount.get()), mins, seconds, totalMillis, NUMBER_FORMAT.format(itemsPerSecond));
        };
    }
}
