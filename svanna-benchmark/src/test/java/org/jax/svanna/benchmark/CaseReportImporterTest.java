package org.jax.svanna.benchmark;

import org.jax.svanna.core.reference.SvannaVariant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.CoordinateSystem;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CaseReportImporterTest {

    @Test
    public void sequenceCasePath() throws Exception {
        Path path = Path.of(CaseReportImporterTest.class.getResource("Witzel-2017-SMARCD2-BII.1.json").toURI());
        List<CaseReport> caseReports = CaseReportImporter.readCasesProvidedAsPositionalArguments(List.of(path));

        assertThat(caseReports, hasSize(1));

        CaseReport report = caseReports.get(0);
        assertThat(report.caseSummary().caseSummary(), equalTo("Witzel-2017-28369036-SMARCD2-BII.1"));

        Set<String> terms = report.patientTerms().stream().map(TermId::getValue).collect(Collectors.toSet());
        assertThat(terms, hasItems("HP:0002164", "HP:0030431", "HP:0002041", "HP:0001263", "HP:0032434", "HP:0010314",
                "HP:0001508", "HP:0032152", "HP:0100806", "HP:0000956", "HP:0001763", "HP:0001007", "HP:0000698"));

        Set<String> variantIds = report.variants().stream().map(SvannaVariant::id).collect(Collectors.toSet());
        assertThat(variantIds, hasItems("causal:dea7c5a2f59009cce209e9137dba0890"));
    }

    @Test
    public void symbolicCasePath() throws Exception {
        Path path = Path.of(CaseReportImporterTest.class.getResource("Hsiao-2015-NF1-UAB-1.json").toURI());
        List<CaseReport> caseReports = CaseReportImporter.readCasesProvidedAsPositionalArguments(List.of(path));

        assertThat(caseReports, hasSize(1));

        CaseReport report = caseReports.get(0);
        assertThat(report.caseSummary().caseSummary(), equalTo("Hsiao-2015-26189818-NF1-UAB_1"));

        Set<String> terms = report.patientTerms().stream().map(TermId::getValue).collect(Collectors.toSet());
        assertThat(terms, hasItems("HP:0007565", "HP:0009732", "HP:0009735", "HP:0009736"));
    }

    @Test
    public void parseBreakendCase() throws Exception {
        Path path = Path.of(CaseReportImporterTest.class.getResource("Iqbal-2013-ANK3-V:1.json").toURI());
        List<CaseReport> caseReports = CaseReportImporter.readCasesProvidedAsPositionalArguments(List.of(path));

        assertThat(caseReports, hasSize(1));

        CaseReport report = caseReports.get(0);
        assertThat(report.caseSummary().caseSummary(), equalTo("Iqbal-2013-23390136-ANK3-V:1"));

        Set<String> terms = report.patientTerms().stream().map(TermId::getValue).collect(Collectors.toSet());
        assertThat(terms, hasItems("HP:0000750", "HP:0002360", "HP:0000729", "HP:0007018", "HP:0001249"));
    }

    @Test
    @Disabled
    public void allCasesPath() {
        Path caseFolderPath = Paths.get("/home/ielis/data/clinical-long-read-genome/phenopackets");
        List<CaseReport> caseReports = CaseReportImporter.readCasesProvidedViaCaseFolderOption(caseFolderPath);
        for (CaseReport report : caseReports) {
            Collection<SvannaVariant> variants = report.variants();
            for (SvannaVariant variant : variants) {
                System.err.printf("%s\t%s\t%d\t%d\t%s\t%s\t%s\t%s\t%s\t%s\n",
                        report.caseSummary(), variant.contigName(), variant.start(), variant.end(), variant.ref(), variant.alt(),
                        variant.zygosity(), variant.minDepthOfCoverage(), variant.numberOfRefReads(), variant.numberOfAltReads());
            }
        }
    }

    @Test
    @Disabled
    public void curatedCasesSummary() {
        // used to generate Phenopacket table
        Path caseFolderPath = Paths.get("/home/ielis/data/clinical-long-read-genome/phenopackets");
        List<CaseReport> caseReports = CaseReportImporter.readCasesProvidedViaCaseFolderOption(caseFolderPath).stream()
                .sorted(Comparator.comparing(cr -> cr.caseSummary().firstAuthor()))
                .collect(Collectors.toList());
        for (CaseReport report : caseReports) {
            CaseSummary caseSummary = report.caseSummary();
            for (SvannaVariant variant : report.variants()) {
                var line = String.join("\t",
                        caseSummary.firstAuthor(), caseSummary.year(), caseSummary.pmid(), caseSummary.gene(),
                        String.format("%s:%d-%d", variant.contigName(), variant.startWithCoordinateSystem(CoordinateSystem.zeroBased()), variant.endWithCoordinateSystem(CoordinateSystem.zeroBased())),
                        variant.variantType().toString());
                System.err.println(line);
            }
        }
    }

}