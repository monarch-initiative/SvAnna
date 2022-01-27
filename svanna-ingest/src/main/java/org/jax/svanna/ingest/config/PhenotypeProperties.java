package org.jax.svanna.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "svanna.ingest.phenotype")
public class PhenotypeProperties {

    private String hpoJsonUrl;
    private String hpoAnnotationsUrl;
    private String mim2geneMedgenUrl;
    private String geneInfoUrl;

    public String hpoJsonUrl() {
        return hpoJsonUrl;
    }

    public void setHpoJsonUrl(String hpoJsonUrl) {
        this.hpoJsonUrl = hpoJsonUrl;
    }

    public String hpoAnnotationsUrl() {
        return hpoAnnotationsUrl;
    }

    public void setHpoAnnotationsUrl(String hpoAnnotationsUrl) {
        this.hpoAnnotationsUrl = hpoAnnotationsUrl;
    }

    public String mim2geneMedgenUrl() {
        return mim2geneMedgenUrl;
    }

    public void setMim2geneMedgenUrl(String mim2geneMedgenUrl) {
        this.mim2geneMedgenUrl = mim2geneMedgenUrl;
    }

    public String geneInfoUrl() {
        return geneInfoUrl;
    }

    public void setGeneInfoUrl(String geneInfoUrl) {
        this.geneInfoUrl = geneInfoUrl;
    }

}
