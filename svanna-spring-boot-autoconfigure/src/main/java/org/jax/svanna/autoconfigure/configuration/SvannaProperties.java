package org.jax.svanna.autoconfigure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "svanna")
public class SvannaProperties {

    /**
     * Path to SvAnna data directory.
     */
    private String dataDirectory;
    private String jannovarCachePath;
    @NestedConfigurationProperty
    private DataProperties data = new DataProperties();
    @NestedConfigurationProperty
    private PrioritizationProperties prioritization = new PrioritizationProperties();


    public String dataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public String jannovarCachePath() {
        return jannovarCachePath;
    }

    public void setJannovarCachePath(String jannovarCachePath) {
        this.jannovarCachePath = jannovarCachePath;
    }

    public DataProperties dataParameters() {
        return data;
    }

    public void setData(DataProperties data) {
        this.data = data;
    }

    public PrioritizationProperties prioritization() {
        return prioritization;
    }

    public void setPrioritization(PrioritizationProperties prioritization) {
        this.prioritization = prioritization;
    }

}
