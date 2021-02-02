package org.jax.svanna.enhancer;

import org.monarchinitiative.phenol.ontology.data.TermId;

public class AnnotatedTissue {

    private final TermId tissueId;

    private final String tissueLabel;

    private final TermId hpoId;

    private final String hpoLabel;

    public AnnotatedTissue(TermId tissueId, String tissue, TermId hpoId, String hpo) {
        this.tissueId = tissueId;
        this.tissueLabel = tissue;
        this.hpoId = hpoId;
        this.hpoLabel = hpo;
    }

    public TermId getTissueId() {
        return tissueId;
    }

    public String getTissueLabel() {
        return tissueLabel;
    }

    public TermId getHpoId() {
        return hpoId;
    }

    public String getHpoLabel() {
        return hpoLabel;
    }
}
