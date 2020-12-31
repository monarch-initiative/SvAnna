package org.jax.svanna.cli.cmd.annotate;

import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategory;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategoryMap;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class HpoTopLevel {

    private final Map<TermId, String> upperLevelTerms;
    private final Map<TermId, String> originalTerms;


    HpoTopLevel(List<TermId> patientTerms, Ontology hpo) {
        Map<TermId, String> builder = new HashMap<>();
        HpoCategoryMap catmap = new HpoCategoryMap();
        catmap.addAnnotatedTerms(patientTerms, hpo);
        for (HpoCategory cat: catmap.getActiveCategoryList()) {
            TermId topLevelHpoId = cat.getTid();
            String label;
            if (hpo.getTermMap().containsKey(topLevelHpoId) ){
                label = hpo.getTermMap().get(topLevelHpoId).getName();
            } else {
                label = "N/A"; // should never happen!
            }
            builder.put(topLevelHpoId, label);
        }
        this.upperLevelTerms = Map.copyOf(builder);
        builder.clear();
        for (TermId tid : patientTerms) {
            String label;
            if (hpo.getTermMap().containsKey(tid) ){
                label = hpo.getTermMap().get(tid).getName();
            } else {
                label = "N/A"; // should never happen!
            }
            builder.put(tid, label);
        }
        this.originalTerms = Map.copyOf(builder);
    }

    Map<TermId, String> getUpperLevelTerms() {
        return upperLevelTerms;
    }

    Map<TermId, String> getOriginalTerms() {
        return originalTerms;
    }
}
