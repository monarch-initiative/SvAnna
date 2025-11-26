package org.monarchinitiative.svanna.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

public interface HpoDiseaseSummary {

    static HpoDiseaseSummary of(TermId diseaseId, String diseaseName) {
        return new HpoDiseaseSummaryDefault(diseaseId, diseaseName);
    }

    TermId getDiseaseId();

    String getDiseaseName();

}
