package org.jax.svanna.autoconfigure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "svanna.data")
public class DataProperties {

    private double tadStabilityThreshold = 80.;

    @NestedConfigurationProperty
    private EnhancerProperties enhancers = new EnhancerProperties();

    public void setEnhancers(EnhancerProperties enhancers) {
        this.enhancers = enhancers;
    }

    public EnhancerProperties enhancers() {
        return enhancers;
    }

    public double tadStabilityThresholdAsPercentage() {
        return tadStabilityThreshold;
    }

    public double tadStabilityThresholdAsFraction() {
        return tadStabilityThreshold / 100.;
    }

    public void setTadStabilityThreshold(double tadStabilityThreshold) {
        this.tadStabilityThreshold = tadStabilityThreshold;
    }

}
