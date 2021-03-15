package org.jax.svanna.cli.cmd.benchmark;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.Variant;

import java.util.Collection;
import java.util.Objects;

class CaseReport {

    private final String caseName;
    private final Collection<TermId> patientTerms;
    private final Collection<Variant> variants;

    CaseReport(String caseName, Collection<TermId> patientTerms, Collection<Variant> variants) {
        this.caseName = caseName;
        this.patientTerms = patientTerms;
        this.variants = variants;
    }

    public String caseName() {
        return caseName;
    }

    public Collection<TermId> patientTerms() {
        return patientTerms;
    }

    public Collection<Variant> variants() {
        return variants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseReport that = (CaseReport) o;
        return Objects.equals(caseName, that.caseName) && Objects.equals(patientTerms, that.patientTerms) && Objects.equals(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseName, patientTerms, variants);
    }

    @Override
    public String toString() {
        return "CaseReport{" +
                "caseName='" + caseName + '\'' +
                ", patientTerms=" + patientTerms +
                ", variants=" + variants +
                '}';
    }
}
