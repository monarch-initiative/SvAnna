package org.jax.svanna.cli.cmd.benchmark;

import java.util.Objects;

/**
 * POJO for the curated case metadata.
 */
public class CaseSummary {

    private final String firstAuthor;
    private final String pmid;
    private final String year;
    private final String gene;
    private final String probandId;

    static CaseSummary of(String firstAuthor, String pmid, String year, String gene, String probandId) {
        return new CaseSummary(firstAuthor, pmid, year, gene, probandId);
    }

    private CaseSummary(String firstAuthor, String pmid, String year, String gene, String probandId) {
        this.firstAuthor = firstAuthor;
        this.pmid = pmid;
        this.year = year;
        this.gene = gene;
        this.probandId = probandId;
    }

    public String firstAuthor() {
        return firstAuthor;
    }

    public String pmid() {
        return pmid;
    }

    public String year() {
        return year;
    }

    public String gene() {
        return gene;
    }

    public String probandId() {
        return probandId;
    }

    public String caseSummary() {
        return String.join("-", firstAuthor, year, pmid, gene, probandId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseSummary that = (CaseSummary) o;
        return Objects.equals(firstAuthor, that.firstAuthor) && Objects.equals(pmid, that.pmid) && Objects.equals(year, that.year) && Objects.equals(gene, that.gene) && Objects.equals(probandId, that.probandId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstAuthor, pmid, year, gene, probandId);
    }

    @Override
    public String toString() {
        return "CaseSummary{" +
                "firstAuthor='" + firstAuthor + '\'' +
                ", pmid='" + pmid + '\'' +
                ", year='" + year + '\'' +
                ", gene='" + gene + '\'' +
                ", probandId='" + probandId + '\'' +
                '}';
    }
}
