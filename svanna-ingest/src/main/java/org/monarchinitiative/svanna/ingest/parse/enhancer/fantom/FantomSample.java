package org.monarchinitiative.svanna.ingest.parse.enhancer.fantom;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

public class FantomSample {
    private final TermId hpoId;
    /** UBERON or CL TermId. */
    private final TermId ontologyId;
    private final String ontologyLabel;
    private final String hpoLabel;
    private final String id;

    public FantomSample(TermId ontologyId, String ontologyLabel, TermId hpoId, String label, String id) {
        this.ontologyId = ontologyId;
        this.ontologyLabel = ontologyLabel;
        this.hpoId = hpoId;
        this.hpoLabel = label;
        this.id = id;
    }

    public TermId getOntologyId() {
        return ontologyId;
    }

    public String getHpoLabel() {
        return hpoLabel;
    }

    public String getId() {
        return id;
    }

    public TermId getHpoId() {
        return hpoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FantomSample that = (FantomSample) o;
        return ontologyId == that.ontologyId &&
                Objects.equals(this.hpoId, that.hpoId) &&
                Objects.equals(hpoLabel, that.hpoLabel) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ontologyId, ontologyLabel, hpoId, hpoLabel, id);
    }

    @Override
    public String toString() {
        return "FantomSample{" +
                "hpoId=" + hpoId +
                ", ontologyId=" + ontologyId +
                ", ontologyLabel='" + ontologyLabel + '\'' +
                ", hpoLabel='" + hpoLabel + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
