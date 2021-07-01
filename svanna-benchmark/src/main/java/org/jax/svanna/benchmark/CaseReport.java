package org.jax.svanna.benchmark;

import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Objects;

class CaseReport {

    private final CaseSummary caseSummary;
    private final Collection<TermId> patientTerms;
    private final Collection<SvannaVariant> variants;

    static CaseReport of(CaseSummary caseSummary, Collection<TermId> patientTerms, Collection<SvannaVariant> variants) {
        return new CaseReport(caseSummary, patientTerms, variants);
    }

    private CaseReport(CaseSummary caseSummary, Collection<TermId> patientTerms, Collection<SvannaVariant> variants) {
        this.caseSummary = caseSummary;
        this.patientTerms = patientTerms;
        this.variants = variants;
    }

    public CaseSummary caseSummary() {
        return caseSummary;
    }

    public Collection<TermId> patientTerms() {
        return patientTerms;
    }

    public Collection<SvannaVariant> variants() {
        return variants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseReport that = (CaseReport) o;
        return Objects.equals(caseSummary, that.caseSummary) && Objects.equals(patientTerms, that.patientTerms) && Objects.equals(variants, that.variants);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseSummary, patientTerms, variants);
    }

    @Override
    public String toString() {
        return "CaseReport{" +
                "caseSummary='" + caseSummary + '\'' +
                ", patientTerms=" + patientTerms +
                ", variants=" + variants +
                '}';
    }
}
