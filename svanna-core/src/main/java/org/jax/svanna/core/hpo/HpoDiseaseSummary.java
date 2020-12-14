package org.jax.svanna.core.hpo;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;

/**
 * This class offers a simple POJO with information about HpoDiseases that are required for output in the
 * list of prioritized structural variants.
 * @author Peter N Robinson
 */
public class HpoDiseaseSummary {

    private final String diseaseId;
    private final String diseaseName;

    /**
     * Extract salient information about the disease and the terms used for prioritization
     * @param disease phenol object representing a disease annotated with HPO terms.
     */
    public HpoDiseaseSummary(HpoDisease disease) {
        this.diseaseId = disease.getDiseaseDatabaseId().getValue();
        this.diseaseName = disease.getName();
    }

    public String getDiseaseId() {
        return diseaseId;
    }

    public String getDiseaseName() {
        return diseaseName;
    }
}
