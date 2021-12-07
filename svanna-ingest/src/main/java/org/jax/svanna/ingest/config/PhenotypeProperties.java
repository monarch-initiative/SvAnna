package org.jax.svanna.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "svanna.ingest.phenotype")
public class PhenotypeProperties {

    private String hpoOboUrl;
    private String hpoAnnotationsUrl;
    private String mim2geneMedgenUrl;
    private String geneInfoUrl;

    public String hpoOboUrl() {
        return hpoOboUrl;
    }

    public void setHpoOboUrl(String hpoOboUrl) {
        this.hpoOboUrl = hpoOboUrl;
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
