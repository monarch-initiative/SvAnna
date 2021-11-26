package org.jax.svanna.ingest.parse.dosage;

import org.jax.svanna.model.landscape.dosage.Dosage;
import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.dosage.DosageSensitivity;
import org.jax.svanna.model.landscape.dosage.DosageSensitivityEvidence;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ClingenGeneCurationParserTest {

    private static final GenomicAssembly ASSEMBLY = GenomicAssemblies.GRCh38p13();

    private static Map<TermId, GenomicRegion> makeGeneByIdMap() {
        return Map.of(
                TermId.of("HGNC:18149"), GenomicRegion.of(ASSEMBLY.contigByName("22"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 42692115, 42721301)),
                TermId.of("HGNC:25662"), GenomicRegion.of(ASSEMBLY.contigByName("15"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 67200667, 67255198)),
                TermId.of("HGNC:20"), GenomicRegion.of(ASSEMBLY.contigByName("16"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 70252295, 70289552)),
                TermId.of("HGNC:21022"), GenomicRegion.of(ASSEMBLY.contigByName("6"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 44297850, 44313358)),
                TermId.of("HGNC:17366"), GenomicRegion.of(ASSEMBLY.contigByName("7"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 122073544, 122144269)),
                TermId.of("HGNC:23"), GenomicRegion.of(ASSEMBLY.contigByName("16"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 8674617, 8784575)),
                TermId.of("HGNC:42"), GenomicRegion.of(ASSEMBLY.contigByName("2"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 168915468, 169031396)),
                TermId.of("HGNC:59"), GenomicRegion.of(ASSEMBLY.contigByName("11"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 17392498, 17476849)),
                TermId.of("HGNC:61"), GenomicRegion.of(ASSEMBLY.contigByName("X"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.oneBased(), 153724851, 153744755))
        );
    }

    private static Map<Integer, Integer> makeNcbiGeneToHgnc() {
        return Map.of(
                53947, 18149, // A4GALT
//                79719, 25662, // AAGAB (commented to simulate missing NCBIGene to HGNC mapping)
                32, 20, // ABCA2
                57505, 21022, // AARS2
                10157, 17366, // AASS
                18, 23, // ABAT
                8647, 42, // ABCB11
                6833, 59, // ABCC8
                215, 61 // ABCD1
        );
    }

    @Test
    public void parse() throws Exception {
        Path geneListPath = Path.of(ClingenGeneCurationParserTest.class.getResource("/dosage/ClinGen_gene_curation_list_GRCh38.15lines.tsv").getPath());
        Map<TermId, GenomicRegion> geneById = makeGeneByIdMap();
        Map<Integer, Integer> ncbiGeneToHgnc = makeNcbiGeneToHgnc();

        ClingenGeneCurationParser parser = new ClingenGeneCurationParser(geneListPath, ASSEMBLY, geneById, ncbiGeneToHgnc);

        List<? extends DosageRegion> elements = parser.parseToList();
        assertThat(elements, hasSize(9));

        DosageRegion first = elements.get(0);
        assertThat(first.geneDosageData().dosages(), hasItem(Dosage.of("A4GALT", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.SOME_EVIDENCE)));

        DosageRegion second = elements.get(1);
        assertThat(second.geneDosageData().dosages(), hasItem(Dosage.of("AAGAB", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE)));
        assertThat(second.contigName(), equalTo("15"));
        assertThat(second.strand(), equalTo(Strand.POSITIVE));
        assertThat(second.coordinateSystem(), equalTo(CoordinateSystem.oneBased()));
        assertThat(second.start(), equalTo(67200667));
        assertThat(second.end(), equalTo(67255200)); // parsed from the ClinGen gene curation list file

        DosageRegion third = elements.get(2);
        assertThat(third.geneDosageData().dosages(), hasItem(Dosage.of("AARS1", DosageSensitivity.TRIPLOSENSITIVITY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE)));

        DosageRegion fourth = elements.get(3);
        assertThat(fourth.geneDosageData().dosages(), hasItem(Dosage.of("AARS2", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.LITTLE_EVIDENCE)));
    }

}