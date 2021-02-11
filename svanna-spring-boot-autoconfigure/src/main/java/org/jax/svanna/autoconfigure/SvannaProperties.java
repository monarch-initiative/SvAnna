package org.jax.svanna.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "svanna")
public class SvannaProperties {

    /**
     * Path to directory with SvAnna files.
     */
    private String dataDirectory;
    private String jannovarCachePath;

    public String jannovarCachePath() {
        return jannovarCachePath;
    }

    public void setJannovarCachePath(String jannovarCachePath) {
        this.jannovarCachePath = jannovarCachePath;
    }

    public String dataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }
}
