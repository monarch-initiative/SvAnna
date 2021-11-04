package org.jax.svanna.ingest.parse.dosage;

import org.jax.svanna.model.landscape.dosage.DosageElement;
import org.jax.svanna.model.landscape.dosage.DosageSensitivity;
import org.jax.svanna.model.landscape.dosage.DosageSensitivityEvidence;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ClingenGeneCurationParserTest {

    private static Map<TermId, GenomicRegion> makeGeneByIdMap() {
        GenomicAssembly assembly = GenomicAssemblies.GRCh38p13();

        return Map.of(
                TermId.of("NCBIGene:53947"), GenomicRegion.of(assembly.contigByName("22"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 42692115, 42721301)),
                TermId.of("NCBIGene:79719"), GenomicRegion.of(assembly.contigByName("15"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 67200667, 67255198)),
                TermId.of("NCBIGene:16"), GenomicRegion.of(assembly.contigByName("16"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 70252295, 70289552)),
                TermId.of("NCBIGene:57505"), GenomicRegion.of(assembly.contigByName("6"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 44297850, 44313358)),
                TermId.of("NCBIGene:10157"), GenomicRegion.of(assembly.contigByName("7"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 122073544, 122144269)),
                TermId.of("NCBIGene:18"), GenomicRegion.of(assembly.contigByName("16"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 8674617, 8784575)),
                TermId.of("NCBIGene:8647"), GenomicRegion.of(assembly.contigByName("2"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 168915468, 169031396)),
                TermId.of("NCBIGene:6833"), GenomicRegion.of(assembly.contigByName("11"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 17392498, 17476849)),
                TermId.of("NCBIGene:215"), GenomicRegion.of(assembly.contigByName("X"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 153724851, 153744755))
        );
    }

    @Test
    public void parse() throws Exception {
        Path geneListPath = Path.of(ClingenGeneCurationParserTest.class.getResource("/dosage/ClinGen_gene_curation_list_GRCh38.15lines.tsv").getPath());
        Map<TermId, GenomicRegion> geneById = makeGeneByIdMap();

        ClingenGeneCurationParser parser = new ClingenGeneCurationParser(geneListPath, geneById);

        List<? extends DosageElement> elements = parser.parseToList();
        assertThat(elements, hasSize(9));

        DosageElement first = elements.get(0);
        assertThat(first.id(), equalTo("A4GALT"));
        assertThat(first.dosageSensitivity(), equalTo(DosageSensitivity.HAPLOINSUFFICIENCY));
        assertThat(first.dosageSensitivityEvidence(), equalTo(DosageSensitivityEvidence.SOME_EVIDENCE));

        DosageElement second = elements.get(1);
        assertThat(second.id(), equalTo("AAGAB"));
        assertThat(second.dosageSensitivity(), equalTo(DosageSensitivity.HAPLOINSUFFICIENCY));
        assertThat(second.dosageSensitivityEvidence(), equalTo(DosageSensitivityEvidence.SUFFICIENT_EVIDENCE));

        DosageElement third = elements.get(2);
        assertThat(third.id(), equalTo("AARS1"));
        assertThat(third.dosageSensitivity(), equalTo(DosageSensitivity.TRIPLOSENSITIVITY));
        assertThat(third.dosageSensitivityEvidence(), equalTo(DosageSensitivityEvidence.SUFFICIENT_EVIDENCE));

        DosageElement fourth = elements.get(3);
        assertThat(fourth.id(), equalTo("AARS2"));
        assertThat(fourth.dosageSensitivity(), equalTo(DosageSensitivity.HAPLOINSUFFICIENCY));
        assertThat(fourth.dosageSensitivityEvidence(), equalTo(DosageSensitivityEvidence.LITTLE_EVIDENCE));
    }

}