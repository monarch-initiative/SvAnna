package org.jax.svanna.core.hpo;

import java.util.Objects;

/**
 * This class offers a simple POJO with information about HpoDiseases that are required for output in the
 * list of prioritized structural variants.
 *
 * @author Peter N Robinson
 */
public class HpoDiseaseSummary {

    private final String diseaseId;
    private final String diseaseName;

    public HpoDiseaseSummary(String diseaseId, String diseaseName) {
        this.diseaseId = Objects.requireNonNull(diseaseId);
        this.diseaseName = Objects.requireNonNull(diseaseName);
    }

    public String getDiseaseId() {
        return diseaseId;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HpoDiseaseSummary that = (HpoDiseaseSummary) o;
        return Objects.equals(diseaseId, that.diseaseId) && Objects.equals(diseaseName, that.diseaseName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(diseaseId, diseaseName);
    }

    @Override
    public String toString() {
        return "HpoDiseaseSummary{" +
                "diseaseId='" + diseaseId + '\'' +
                ", diseaseName='" + diseaseName + '\'' +
                '}';
    }
}
