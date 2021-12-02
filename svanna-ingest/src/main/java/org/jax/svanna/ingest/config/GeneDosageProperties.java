package org.jax.svanna.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "svanna.ingest.dosage")
public class GeneDosageProperties {

    private String geneUrl;
    private String regionUrl;

    public String getGeneUrl() {
        return geneUrl;
    }

    public void setGeneUrl(String geneUrl) {
        this.geneUrl = geneUrl;
    }

    public String getRegionUrl() {
        return regionUrl;
    }

    public void setRegionUrl(String regionUrl) {
        this.regionUrl = regionUrl;
    }

}
