package org.jax.svanna.core.configuration;

import java.nio.file.Path;
import java.util.Objects;

public class SvAnnaProperties {

    private final Path dataDirectory;

    private final PrioritizationProperties prioritizationProperties;
    private final DataProperties dataProperties;

    public static SvAnnaProperties of(Path dataDirectory, PrioritizationProperties prioritizationProperties, DataProperties dataProperties) {
        return new SvAnnaProperties(dataDirectory, prioritizationProperties, dataProperties);
    }

    private SvAnnaProperties(Path dataDirectory, PrioritizationProperties prioritizationProperties, DataProperties dataProperties) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "Data directory must not be null!");
        this.prioritizationProperties = Objects.requireNonNull(prioritizationProperties, "Prioritization properties must not be null!");
        this.dataProperties = Objects.requireNonNull(dataProperties, "Data properties must not be null!");
    }

    public Path dataDirectory() {
        return dataDirectory;
    }

    public PrioritizationProperties prioritizationProperties() {
        return prioritizationProperties;
    }

    public DataProperties dataProperties() {
        return dataProperties;
    }

    @Override
    public String toString() {
        return "SvAnnaProperties{" +
                "dataDirectory=" + dataDirectory +
                ", prioritizationProperties=" + prioritizationProperties +
                ", dataProperties=" + dataProperties +
                '}';
    }
}
