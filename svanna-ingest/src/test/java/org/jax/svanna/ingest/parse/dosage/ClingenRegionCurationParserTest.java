package org.jax.svanna.ingest.parse.dosage;

import org.jax.svanna.model.landscape.dosage.Dosage;
import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.dosage.DosageSensitivity;
import org.jax.svanna.model.landscape.dosage.DosageSensitivityEvidence;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.GenomicAssemblies;

import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ClingenRegionCurationParserTest {

    @Test
    public void parse() throws Exception {
        Path regionPath = Path.of(ClingenRegionCurationParserTest.class.getResource("/dosage/ClinGen_region_curation_list_GRCh38.15lines.tsv").getPath());
        ClingenRegionCurationParser parser = new ClingenRegionCurationParser(regionPath, GenomicAssemblies.GRCh38p13());

        List<? extends DosageRegion> elements = parser.parseToList();
        assertThat(elements, hasSize(3));

        DosageRegion first = elements.get(0);
        assertThat(first.dosage(), equalTo(Dosage.of("ISCA-46739", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE)));

        DosageRegion second = elements.get(1);
        assertThat(second.dosage(), equalTo(Dosage.of("ISCA-46734", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE)));

        DosageRegion third = elements.get(2);
        assertThat(third.dosage(), equalTo(Dosage.of("ISCA-46734", DosageSensitivity.TRIPLOSENSITIVITY, DosageSensitivityEvidence.SOME_EVIDENCE)));

        DosageRegion fourth = elements.get(3);
        assertThat(fourth.dosage(), equalTo(Dosage.of("ISCA-46731", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE)));
    }
}