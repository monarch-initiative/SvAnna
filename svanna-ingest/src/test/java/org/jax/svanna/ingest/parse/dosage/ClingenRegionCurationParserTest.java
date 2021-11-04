package org.jax.svanna.ingest.parse.dosage;

import org.jax.svanna.model.landscape.dosage.DosageElement;
import org.jax.svanna.model.landscape.dosage.DosageSensitivity;
import org.jax.svanna.model.landscape.dosage.DosageSensitivityEvidence;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.GenomicAssemblies;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ClingenRegionCurationParserTest {

    @Test
    public void parse() throws Exception {
        Path regionPath = Path.of(ClingenRegionCurationParserTest.class.getResource("/dosage/ClinGen_region_curation_list_GRCh38.15lines.tsv").getPath());
        ClingenRegionCurationParser parser = new ClingenRegionCurationParser(regionPath, GenomicAssemblies.GRCh38p13());

        List<? extends DosageElement> elements = parser.parseToList();
        assertThat(elements, hasSize(4));

        DosageElement first = elements.get(0);
        assertThat(first.id(), equalTo("ISCA-46739"));
        assertThat(first.dosageSensitivity(), equalTo(DosageSensitivity.HAPLOINSUFFICIENCY));
        assertThat(first.dosageSensitivityEvidence(), equalTo(DosageSensitivityEvidence.SUFFICIENT_EVIDENCE));

        DosageElement second = elements.get(1);
        assertThat(second.id(), equalTo("ISCA-46734"));
        assertThat(second.dosageSensitivity(), equalTo(DosageSensitivity.HAPLOINSUFFICIENCY));
        assertThat(second.dosageSensitivityEvidence(), equalTo(DosageSensitivityEvidence.SUFFICIENT_EVIDENCE));

        DosageElement third = elements.get(2);
        assertThat(third.id(), equalTo("ISCA-46734"));
        assertThat(third.dosageSensitivity(), equalTo(DosageSensitivity.TRIPLOSENSITIVITY));
        assertThat(third.dosageSensitivityEvidence(), equalTo(DosageSensitivityEvidence.SOME_EVIDENCE));

        DosageElement fourth = elements.get(3);
        assertThat(fourth.id(), equalTo("ISCA-46731"));
        assertThat(fourth.dosageSensitivity(), equalTo(DosageSensitivity.HAPLOINSUFFICIENCY));
        assertThat(fourth.dosageSensitivityEvidence(), equalTo(DosageSensitivityEvidence.SUFFICIENT_EVIDENCE));
    }
}