package org.monarchinitiative.svanna.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "svanna.ingest.enhancers")
public class EnhancerProperties {

    private String vista;

    private String fantomMatrix;

    private String fantomSample;

    public String vista() {
        return vista;
    }

    public void setVista(String vista) {
        this.vista = vista;
    }

    public String fantomMatrix() {
        return fantomMatrix;
    }

    public void setFantomMatrix(String fantomMatrix) {
        this.fantomMatrix = fantomMatrix;
    }

    public String fantomSample() {
        return fantomSample;
    }

    public void setFantomSample(String fantomSample) {
        this.fantomSample = fantomSample;
    }

}
