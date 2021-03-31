package org.jax.svanna.core.overlap;


import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.reference.GeneService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(classes = TestDataConfig.class)
public class IntervalArrayGeneOverlapperTest {


    @Autowired
    private GenomicAssembly genomicAssembly;


    @Autowired
    public GeneService geneService;

    @ParameterizedTest
    @CsvSource({
            "133356544, C,  '', false",
            "133356545, A,  '', true",
            "133356546, G,  '', false",
    })
    public void getOverlaps_tss_deletion(int pos, String ref, String alt, boolean expected) {
        Contig chr9 = genomicAssembly.contigByName("9");
        Variant variant = Variant.of(chr9, "TSS_DEL", Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(pos), ref, alt);

        IntervalArrayGeneOverlapper overlapper = new IntervalArrayGeneOverlapper(geneService.getChromosomeMap());
        List<GeneOverlap> overlaps = overlapper.getOverlaps(variant);
        assertThat(overlaps.stream().map(GeneOverlap::overlapType).allMatch(ot -> ot.equals(OverlapType.AFFECTS_CODING_TRANSCRIPT_TSS)), equalTo(expected));
    }
}