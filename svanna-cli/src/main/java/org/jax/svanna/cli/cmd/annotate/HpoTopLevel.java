package org.jax.svanna.cli.cmd.annotate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategory;
import org.monarchinitiative.phenol.annotations.formats.hpo.category.HpoCategoryMap;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HpoTopLevel {

    private final Map<TermId, String> upperLevelTerms;
    private final Map<TermId, String> originalTerms;


    public HpoTopLevel(List<TermId> patientTerms, Ontology hpo) {
        ImmutableMap.Builder<TermId, String> builder = new ImmutableMap.Builder<>();
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
        this.upperLevelTerms = builder.build();
        builder = new ImmutableMap.Builder<>();
        for (TermId tid : patientTerms) {
            String label;
            if (hpo.getTermMap().containsKey(tid) ){
                label = hpo.getTermMap().get(tid).getName();
            } else {
                label = "N/A"; // should never happen!
            }
            builder.put(tid, label);
        }
        this.originalTerms = builder.build();
    }

    public Map<TermId, String> getUpperLevelTerms() {
        return upperLevelTerms;
    }

    public Map<TermId, String> getOriginalTerms() {
        return originalTerms;
    }
}
