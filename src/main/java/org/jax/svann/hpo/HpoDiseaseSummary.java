package org.jax.svann.hpo;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

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
     * @param disease
     */
    public HpoDiseaseSummary(HpoDisease disease, List<TermId> hpoIdList) {
        this.diseaseId = disease.getDiseaseDatabaseId().getValue();
        this.diseaseName = disease.getName();
    }
}
