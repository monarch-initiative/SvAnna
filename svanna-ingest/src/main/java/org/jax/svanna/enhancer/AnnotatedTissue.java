package org.jax.svanna.enhancer;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnotatedTissue that = (AnnotatedTissue) o;
        return Objects.equals(tissueId, that.tissueId) &&
                Objects.equals(tissueLabel, that.tissueLabel) &&
                Objects.equals(hpoId, that.hpoId) &&
                Objects.equals(hpoLabel, that.hpoLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tissueId, tissueLabel, hpoId, hpoLabel);
    }

    @Override
    public String toString() {
        return "AnnotatedTissue{" +
                "tissueId=" + tissueId +
                ", tissueLabel='" + tissueLabel + '\'' +
                ", hpoId=" + hpoId +
                ", hpoLabel='" + hpoLabel + '\'' +
                '}';
    }

    public String summary() {
        return String.format("%s[%s;%s;%s]", getTissueId().getValue() ,getTissueLabel(), getHpoId().getValue(), getHpoLabel());
    }
}
