package org.jax.svanna.core.overlap;

import org.jax.svanna.core.TestDataConfig;
import org.jax.svanna.core.reference.TranscriptService;
import org.jax.svanna.test.TestVariants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.jax.svanna.core.overlap.OverlapType.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestDataConfig.class)
@Disabled // TODO - evaluate whether this overlapper should be removed
public class SvAnnOverlapperTest {

    @Autowired
    public GenomicAssembly assembly;

    @Autowired
    public TestVariants testVariants;

    @Autowired
    public TranscriptService transcriptService;

    private SvAnnOverlapper overlapper;

    @BeforeEach
    public void setUp() {
        overlapper = new SvAnnOverlapper(transcriptService.getChromosomeMap());
    }

    @Test
    public void testGetIntronicDistance() {
        Variant surf2insertionIntron3 = testVariants.deletions().surf2WithinAnIntron();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf2insertionIntron3);

        TranscriptOverlap overlap = overlaps.get(0);
        // there are 249 bases between the deletion and the downstream exon, not including deletion or exonic regions
        // there are 1186 bases between the deletion and the upstream exon, not including deletion or exonic regions
        assertEquals("NM_017503.4", overlap.getAccession());
        assertEquals(249, overlap.getDistance());
    }


    /**
     * We expect two overlapping transcripts for the SURF2 single exon deletion
     */
    @Test
    public void testSurf2Exon3Overlaps() {
        Variant surf1Exon3Deletion = testVariants.deletions().surf2singleExon_exon3();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf1Exon3Deletion);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(SINGLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
            assertTrue(o.overlapsCds());
        }
    }


    /**
     * We expect three overlapping transcripts for the SURF1 two-exon deletion
     */
    @Test
    public void testSurf1TwoExonDeletion() {
        Variant twoExonSurf1 = testVariants.deletions().surf1TwoExon_exons_6_and_7();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(twoExonSurf1);

        assertEquals(3, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_003172.3", "NM_001280787.1", "XM_011518942.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(MULTIPLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
            assertTrue(o.overlapsCds());
        }
    }


    /**
     * SURF1:NM_003172.4 entirely deleted, SURF2:NM_017503.5 partially deleted
     * chr9:133_350_001-133_358_000
     * Note that in the application, the SvPrioritizer will recognize that two different genes
     * are affected but the Overlapper does not do that
     */
    @Test
    public void testTwoTranscriptDeletion() {
        Variant surf1and2deletion = testVariants.deletions().surf1Surf2oneEntireTranscriptAndPartOfAnother();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf1and2deletion);
        assertEquals(5, overlaps.size());
        Set<String> expectedAccessionNumbers =
                Set.of("NM_017503.4", "NM_001278928.1", "NM_003172.3", "NM_001280787.1", "XM_011518942.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            if (o.getAccession().equals("NM_017503.4")) { // might be the other
                assertEquals(TRANSCRIPT_CONTAINED_IN_SV, o.getOverlapType());
            } else if (o.getAccession().equals("NM_003172.3")) { // might be the other
                assertEquals(MULTIPLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
            }
            assertTrue(o.overlapsCds());
        }
    }


    /**
     * Deletion within an intron.
     * <p>
     * SURF2:NM_017503.5 700bp deletion within intron 3
     * chr9:133_359_001-133_359_700
     */
    @Test
    public void testDeletionWithinAnIntron() {
        Variant surf1DeletionWithinIntron = testVariants.deletions().surf2WithinAnIntron();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf1DeletionWithinIntron);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(INTRONIC, o.getOverlapType());
            assertFalse(o.overlapsCds());
        }
    }


    /**
     * Deletion in 5UTR.
     * <p>
     * SURF2:NM_017503.5 20bp deletion in 5UTR
     * chr9:133_356_561-133_356_580
     * <p>
     * The deletion is thus EXONIC but NOT CDS
     */
    @Test
    public void testDeletionIn5UTR() {
        Variant surf1DeletionWithinIntron = testVariants.deletions().surf2In5UTR();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf1DeletionWithinIntron);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(NON_CDS_REGION_IN_SINGLE_EXON, o.getOverlapType());
            assertFalse(o.overlapsCds());
        }
    }


    /**
     * Deletion in 3UTR.
     * <p>
     * SURF1:NM_003172.4 100bp deletion in 3UTR
     * chr9:133_351_801-133_351_900
     * <p>
     * The deletion is thus EXONIC but NOT CDS
     */
    @Test
    public void testDeletionIn3UTR() {
        Variant surf1DeletionWithinIntron = testVariants.deletions().surf1In3UTR();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf1DeletionWithinIntron);
        assertEquals(3, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_003172.3", "NM_001280787.1", "XM_011518942.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(NON_CDS_REGION_IN_SINGLE_EXON, o.getOverlapType());
            assertFalse(o.overlapsCds());
        }
    }


    /**
     * Deletion downstream intergenic.
     * <p>
     * SURF1:NM_003172.4 downstream, 10kb deletion
     * chr9:133_300_001-133_310_000
     * <p>
     * Note that the Jannovar API returns only one transcript for upstream/downstream matches, even though
     * all three SURF1 transcripts start at the same location. In our test we are getting
     * "NM_003172.3", but it is safer to test that we get the expected symbol
     */
    @Test
    public void testDeletionDownstreamIntergenic() {
        Variant surf1Downstream = testVariants.deletions().surf1DownstreamIntergenic();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf1Downstream);
        assertEquals(1, overlaps.size());
        TranscriptOverlap overlap = overlaps.get(0);
        assertEquals("NM_003172.4", overlap.getAccession());
        assertEquals(DOWNSTREAM_GENE_VARIANT_500KB, overlap.getOverlapType());
        assertFalse(overlap.overlapsCds());
    }


    /**
     * Deletion upstream intergenic. FBN1
     * <p>
     * hg38 chr15:48,408,306-48,645,849 Size: 237,544 Total Exon Count: 66 Strand: -
     * upstream, 10kb deletion
     * chr15:48_655_000-48_665_000
     */
    @Test
    public void testDeletionUpstreamIntergenic() {
        Variant brca2Upstream = testVariants.deletions().brca2UpstreamIntergenic();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(brca2Upstream);
        assertEquals(1, overlaps.size());
        TranscriptOverlap overlap = overlaps.get(0);
        assertEquals("BRCA2", overlap.getAccession());
        assertEquals(UPSTREAM_GENE_VARIANT_500KB, overlap.getOverlapType());
        assertFalse(overlap.overlapsCds());
    }


    /*
     *
     *                                         INSERTIONS
     *
     */

    /**
     * Insertion in 5'UTR.
     * <p>
     * SURF2:NM_017503.5 10bp insertion in 5UTR
     * chr9:133_356_571-133_356_571
     * The insertion overlaps with a single exon, noncoding.
     * Note that we do not test the SvType here, that is not the purpose
     * of the overlap class
     */
    @Test
    public void testInsertionIn5UTR() {
        Variant surf2insertion5utr = testVariants.insertions().surf2InsertionIn5UTR();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf2insertion5utr);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(NON_CDS_REGION_IN_SINGLE_EXON, o.getOverlapType());
            assertFalse(o.overlapsCds());
        }
    }


    /**
     * Insertion in 3'UTR
     * <p>
     * SURF1:NM_003172.4 10bp insertion in 3UTR
     * chr9:133_351_851-133_351_851
     */
    @Test
    public void testInsertionIn3UTR() {
        Variant surf2insertion3utr = testVariants.insertions().surf1InsertionIn3UTR();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf2insertion3utr);
        assertEquals(3, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_003172.3", "NM_001280787.1", "XM_011518942.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(NON_CDS_REGION_IN_SINGLE_EXON, o.getOverlapType());
            assertFalse(o.overlapsCds());
        }
    }


    /**
     * Insertion in exon.
     * <p>
     * SURF2:NM_017503.5 10bp insertion in exon 4
     * chr9:133_360_001-133_360_001
     */

    @Test
    public void testInsertionInExon4() {
        Variant surf2insertionExon4 = testVariants.insertions().surf2Exon4();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf2insertionExon4);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(SINGLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
            assertTrue(o.overlapsCds());
        }
    }


    /**
     * Insertion in intron.
     * <p>
     * SURF2:NM_017503.5 10bp insertion in intron 3
     * chr9:133_359_001-133_359_001
     * <p>
     * INTRONIC, NON-CDS
     */
    @Test
    public void testInsertionInIntron3() {
        Variant surf2insertionIntron3 = testVariants.insertions().surf2Intron3();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(surf2insertionIntron3);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(INTRONIC, o.getOverlapType());
            assertFalse(o.overlapsCds());
        }
    }


    /*
     *
     *                                         INVERSIONS
     *
     */

    /**
     * Inversion in exon and intron.
     * <p>
     * GCK:NM_000162.4 100bp inversion in exon 2 and part of intron 1
     * chr7:44_153_401-44_153_600
     * <p>
     * INTRONIC, NON-CDS
     */
    @Test
    public void testInversionInExon3() {
        Variant gckExonic = testVariants.inversions().gckExonic();
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(gckExonic);

        assertEquals(4, overlaps.size());
        assertThat(overlaps.stream().map(TranscriptOverlap::getAccession).collect(Collectors.toSet()), hasItems("NM_000162.4", "NM_033507.2", "NM_033508.2", "NM_001354800.1"));
        for (var o : overlaps) {
            assertEquals(SINGLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
            assertTrue(o.overlapsCds());
        }
    }

    /*
     *
     *                                         TRANSLOCATIONS
     *
     */

    /**
     * Translocation where one CDS is disrupted and the other is not
     * <p>
     * left mate, SURF2:NM_017503.5 intron 3 (disrupted CDS)
     * chr9:133_359_000 (+)
     * right mate, upstream from BRCA2 (not disrupted)
     * chr13:32_300_000 (+)
     */
    @Test
    public void translocationWhereOneCdsIsDisruptedAndTheOtherIsNot() {
        Breakend left = Breakend.of(assembly.contigByName("9"), "tra_l", Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(133_359_001), Position.of(133_359_000));
        Breakend right = Breakend.of(assembly.contigByName("13"), "tra_r", Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(32_300_001), Position.of(32_300_000));

        Variant translocation = BreakendVariant.of("translocation_where_one_cds_is_disrupted_and_the_other_is_not", left, right, "G", "");
        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(translocation);

        assertThat(overlaps, hasSize(3));

        TranscriptOverlap surf2_NM_017503_4 = overlaps.get(0);
        assertThat(surf2_NM_017503_4.getOverlapType(), equalTo(INTRONIC));
        assertThat(surf2_NM_017503_4.getDistance(), equalTo(949));

        TranscriptOverlap surf2_NM_001278928_1 = overlaps.get(1);
        assertThat(surf2_NM_001278928_1.getOverlapType(), equalTo(INTRONIC));
        assertThat(surf2_NM_001278928_1.getDistance(), equalTo(949));

        TranscriptOverlap brca2_NM_000059_3 = overlaps.get(2);
        assertThat(brca2_NM_000059_3.getOverlapType(), equalTo(UPSTREAM_GENE_VARIANT_500KB));
        assertThat(brca2_NM_000059_3.getDistance(), equalTo(-15479));
    }

    /**
     * Translocation where no transcript is disrupted.
     * <p>
     * <ul>
     *   <li><b>left mate:</b> SURF2:NM_017503.5 (~30bp upstream from TSS), <code>chr9:133_356_520 (+)</code></li>
     *   <li><b>right mate:</b> upstream from BRCA2 (not disrupted), <code>chr13:32_300_000 (+)</code></li>
     * </ul>
     */
    @Test
    public void intergenicTranslocationYieldsNoOverlaps() {
        Breakend left = Breakend.of(assembly.contigByName("9"), "tra_l", Strand.POSITIVE, CoordinateSystem.oneBased(), Position.of(133_356_521), Position.of(133_356_520));
        Breakend right = Breakend.of(assembly.contigByName("13"), "tra_r", Strand.POSITIVE,  CoordinateSystem.oneBased(), Position.of(32_300_001), Position.of(32_300_000));
        Variant translocation = BreakendVariant.of("intergenic_translocation", left, right, "C", "");

        List<TranscriptOverlap> overlaps = overlapper.getOverlaps(translocation);

        assertThat(overlaps, hasSize(3));
    }
}