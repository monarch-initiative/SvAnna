package org.jax.svanna.core.overlap;


import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.service.GeneService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.*;
import org.monarchinitiative.svart.util.VcfBreakendResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(classes = TestDataConfig.class)
public class IntervalArrayGeneOverlapperTest {


    @Autowired
    public GeneService geneService;
    @Autowired
    private GenomicAssembly genomicAssembly;

    @ParameterizedTest
    @CsvSource({
            "133356549, T,  '', false",
            "133356550, C,  '', true",
            "133356551, T,  '', false",
    })
    public void getOverlaps_tss_deletion(int pos, String ref, String alt, boolean expected) {
        Contig chr9 = genomicAssembly.contigByName("9");
        Variant variant = Variant.of(chr9, "TSS_DEL", Strand.POSITIVE, CoordinateSystem.oneBased(), pos, ref, alt);

        IntervalArrayGeneOverlapper overlapper = new IntervalArrayGeneOverlapper(geneService.getChromosomeMap());
        List<GeneOverlap> surf2Overlaps = overlapper.getOverlaps(variant).stream()
                .filter(go -> "SURF2".equals(go.gene().symbol()))
                .collect(Collectors.toUnmodifiableList());
        boolean match = !surf2Overlaps.isEmpty()
                && surf2Overlaps.stream()
                .map(GeneOverlap::overlapType)
                .allMatch(ot -> ot.equals(OverlapType.AFFECTS_CODING_TRANSCRIPT_TSS));

        assertThat(match, equalTo(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "chrY,                 left, right, 2787000, -56, 20, C, C[chr16_KI270856v1_alt:58[, 'SRY'",
            "chr16_KI270856v1_alt, left, right,      58, -57, 16, T, ]chrY:2787000]T,            'SRY'",
    })
    public void getOverlaps_breakendVariantWithMateOnAltContig(String contigName, String id, String mateId, int pos, int ciPosStart, int ciPosEnd, String ref, String alt, String geneSymbols) {
        /*
        chr16	1871875	pbsv.BND.chr16:1871875-chr16_KI270856v1_alt:58	C	C[chr16_KI270856v1_alt:58[	.	NearContigEnd	CIPOS=-56,20;MATEID=pbsv.BND.chr16_KI270856v1_alt:58-chr16:1871875;SVTYPE=BND;TADSV=0.00	GT:AD:DP	0/1:9,8:17
        chr16_KI270856v1_alt	58	pbsv.BND.chr16_KI270856v1_alt:58-chr16:1871875	T	]chr16:1871875]T	.	NearContigEnd	CIPOS=-57,19;MATEID=pbsv.BND.chr16:1871875-chr16_KI270856v1_alt:58;SVTYPE=BND;TADSV=0.00	GT:AD:DP	0/1:3,8:11
         */
        VcfBreakendResolver breakendResolver = new VcfBreakendResolver(genomicAssembly);
        BreakendVariant variant = breakendResolver.resolve(
                "EVENT", id, mateId,
                genomicAssembly.contigByName(contigName), pos, ConfidenceInterval.of(ciPosStart, ciPosEnd), ConfidenceInterval.precise(),
                ref, alt);

        IntervalArrayGeneOverlapper overlapper = new IntervalArrayGeneOverlapper(geneService.getChromosomeMap());
        List<GeneOverlap> overlaps = overlapper.getOverlaps(variant);
        assertThat(overlaps.stream().map(overlap -> overlap.gene().symbol()).collect(Collectors.joining("|")), equalTo(geneSymbols));
    }
}