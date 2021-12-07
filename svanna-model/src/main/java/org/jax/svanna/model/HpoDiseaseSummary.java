package org.jax.svanna.model;

public interface HpoDiseaseSummary {

    static HpoDiseaseSummary of(String diseaseId, String diseaseName) {
        return new HpoDiseaseSummaryDefault(diseaseId, diseaseName);
    }

    String getDiseaseId();

    String getDiseaseName();

}
