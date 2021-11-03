package org.jax.svanna.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "svanna.ingest.variants")
public class VariantProperties {

    private String dgvUrl;
    private String gnomadSvRegionsUrl;
    private String gnomadSvVcfUrl;
    private String hgsvc2VcfUrl;

    private String dbsnpVcfUrl;

    public String hgsvc2VcfUrl() {
        return hgsvc2VcfUrl;
    }

    public void setHgsvc2VcfUrl(String hgsvc2VcfUrl) {
        this.hgsvc2VcfUrl = hgsvc2VcfUrl;
    }

    public String gnomadSvVcfUrl() {
        return gnomadSvVcfUrl;
    }

    public void setGnomadSvVcfUrl(String gnomadSvVcfUrl) {
        this.gnomadSvVcfUrl = gnomadSvVcfUrl;
    }

    public String gnomadSvRegionsUrl() {
        return gnomadSvRegionsUrl;
    }

    public void setGnomadSvRegionsUrl(String gnomadSvRegionsUrl) {
        this.gnomadSvRegionsUrl = gnomadSvRegionsUrl;
    }

    public String dgvUrl() {
        return dgvUrl;
    }

    public void setDgvUrl(String dgvUrl) {
        this.dgvUrl = dgvUrl;
    }

    public String dbsnpVcfUrl() {
        return dbsnpVcfUrl;
    }

    public void setDbsnpVcfUrl(String dbsnpVcfUrl) {
        this.dbsnpVcfUrl = dbsnpVcfUrl;
    }
}
