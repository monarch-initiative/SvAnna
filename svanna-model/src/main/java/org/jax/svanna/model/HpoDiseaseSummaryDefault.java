package org.jax.svanna.model;

import java.util.Objects;

class HpoDiseaseSummaryDefault implements HpoDiseaseSummary {

    private final String diseaseId;
    private final String diseaseName;

    HpoDiseaseSummaryDefault(String diseaseId, String diseaseName) {
        this.diseaseId = Objects.requireNonNull(diseaseId, "Disease ID must not be null");
        this.diseaseName = Objects.requireNonNull(diseaseName, "Disease name must not be null");
    }

    @Override
    public String getDiseaseId() {
        return diseaseId;
    }

    @Override
    public String getDiseaseName() {
        return diseaseName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HpoDiseaseSummaryDefault that = (HpoDiseaseSummaryDefault) o;
        return Objects.equals(diseaseId, that.diseaseId) && Objects.equals(diseaseName, that.diseaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(diseaseId, diseaseName);
    }

    @Override
    public String toString() {
        return "HpoDiseaseSummaryDefault{" +
                "diseaseId='" + diseaseId + '\'' +
                ", diseaseName='" + diseaseName + '\'' +
                '}';
    }
}
