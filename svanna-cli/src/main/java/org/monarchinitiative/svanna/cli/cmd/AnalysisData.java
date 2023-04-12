package org.monarchinitiative.svanna.cli.cmd;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * SvAnna requires these inputs for the analysis.
 */
class AnalysisData {

    private final List<TermId> phenotypeTerms;
    private final Path vcf;

    AnalysisData(List<TermId> phenotypeTerms, Path vcf) {
        this.phenotypeTerms = phenotypeTerms;
        this.vcf = vcf;
    }

    List<TermId> phenotypeTerms() {
        return phenotypeTerms;
    }

    Path vcf() {
        return vcf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalysisData that = (AnalysisData) o;
        return Objects.equals(phenotypeTerms, that.phenotypeTerms) && Objects.equals(vcf, that.vcf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phenotypeTerms, vcf);
    }

    @Override
    public String toString() {
        return "AnalysisData{" +
                "phenotypeTerms=" + phenotypeTerms +
                ", vcf=" + vcf +
                '}';
    }
}
