package org.jax.svanna.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "svanna.ingest.genes")
public class GeneProperties {

    private String gencodeGtfUrl;
    private String hgncIdToNcbiGenePath;

    public String gencodeGtfUrl() {
        return gencodeGtfUrl;
    }

    public void setGencodeGtfUrl(String gencodeGtfUrl) {
        this.gencodeGtfUrl = gencodeGtfUrl;
    }

    public String hgncIdToNcbiGenePath() {
        return hgncIdToNcbiGenePath;
    }

    public void setHgncIdToNcbiGenePath(String hgncIdToNcbiGenePath) {
        this.hgncIdToNcbiGenePath = hgncIdToNcbiGenePath;
    }
}
