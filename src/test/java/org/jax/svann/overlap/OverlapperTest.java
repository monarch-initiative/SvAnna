package org.jax.svann.overlap;

import org.jax.svann.TestBase;
import org.jax.svann.reference.SequenceRearrangement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jax.svann.overlap.OverlapType.*;
import static org.jax.svann.parse.TestVariants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that we get the correct overlaps with respect to a small set of transcript models in our
 * "mini" Jannovar Data. These are the genes and transcripts that are contained:
 * SURF1: NM_003172.3,NM_001280787.1, XM_011518942.1
 * SURF2: NM_017503.4, NM_001278928.1
 * FBN1 NM_000138.4
 * ZNF436: NM_030634.2, NM_001077195.1,XM_011542201.1,XR_001737137.2,XR_001737135.2,
 * ZBTB48: XR_001737136.1, XM_017001110.2, XM_017001111.2, XM_017001112.2, XM_017001113.2, XM_017001114.2,
 *    NM_005341.3, XR_946621.3, NM_001278648.1, NM_001278647.1
 * HNF4A: NM_000457.4, NM_178850.2, NM_178849.2, NM_001258355.1, NM_001030004.2, NM_001030003.2, NM_175914.4,
 *     NM_001287182.1, NM_001287183.1, NM_001287184.1, XM_005260407.4
 * GCK: NM_033507.2, NM_033508.2, NM_000162.4, NM_001354800.1, NM_001354802.1, NM_001354801.1,
 *      NM_001354803.1,XM_024446707.1
 * BRCA2: NM_000059.3
 * COL4A5: XM_017029263.2, XM_017029262.2, XM_017029261.1, XM_017029260.1, XM_017029259.2, NM_033380.2
 *          XM_011530849.2, NM_000495.4
 * SRY: NM_003140.2
 */
public class OverlapperTest extends TestBase {

    private Overlapper overlapper;

    @BeforeEach
    public void setUp() {
        overlapper = new Overlapper(JANNOVAR_DATA);
    }

    /**
     * We expect two overlapping transcripts for the SURF2 single exon deletion
     */
    @Test
    public void testSurf2Exon3Overlaps() {
        SequenceRearrangement surf1Exon3Deletion = Deletions.surf2singleExon_exon3();
        List<Overlap> overlaps = overlapper.getOverlapList(surf1Exon3Deletion);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(SINGLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
            assertTrue(o.overlapsCds());
        }
    }

    /**
     *  We expect three overlapping transcripts for the SURF1 two-exon deletion
     */
    @Test
    public void testSurf1TwoExonDeletion() {
        SequenceRearrangement twoExonSurf1 = Deletions.surf1TwoExon_exons_6_and_7();
        List<Overlap> overlaps = overlapper.getOverlapList(twoExonSurf1);
        assertEquals(3, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_003172.3","NM_001280787.1", "XM_011518942.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
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
     * */
    @Test
    public void testTwoTranscriptDeletion() {
        SequenceRearrangement surf1and2deletion = Deletions.surf1Surf2oneEntireTranscriptAndPartOfAnother();
        List<Overlap> overlaps = overlapper.getOverlapList(surf1and2deletion);
        assertEquals(5, overlaps.size());
        Set<String> expectedAccessionNumbers =
                Set.of("NM_017503.4", "NM_001278928.1","NM_003172.3","NM_001280787.1", "XM_011518942.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            if (o.getGeneSymbol().equals("SURF1"))
                assertEquals(TRANSCRIPT_CONTAINED_IN_SV, o.getOverlapType());
            else if (o.getGeneSymbol().equals("SURF2"))
                assertEquals(MULTIPLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
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
    public void testdeletionWithinAnIntron() {
        SequenceRearrangement surf1DeletionWithinIntron = Deletions.surf2WithinAnIntron();
        List<Overlap> overlaps = overlapper.getOverlapList(surf1DeletionWithinIntron);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
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
     *
     * The deletion is thus EXONIC but NOT CDS
     */
    @Test
    public void testdeletionIn5UTR() {
        SequenceRearrangement surf1DeletionWithinIntron = Deletions.surf2In5UTR();
        List<Overlap> overlaps = overlapper.getOverlapList(surf1DeletionWithinIntron);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(SINGLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
            assertFalse(o.overlapsCds());
        }
    }



    /**
     * Deletion in 3UTR.
     * <p>
     * SURF1:NM_003172.4 100bp deletion in 3UTR
     * chr9:133_351_801-133_351_900
     *
     *  The deletion is thus EXONIC but NOT CDS
     */

    @Test
    public void testDeletionIn3UTR() {
        SequenceRearrangement surf1DeletionWithinIntron = Deletions.surf1In3UTR();
        List<Overlap> overlaps = overlapper.getOverlapList(surf1DeletionWithinIntron);
        assertEquals(3, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_003172.3","NM_001280787.1", "XM_011518942.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(SINGLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
            assertFalse(o.overlapsCds());
        }
    }


    /**
     * Deletion downstream intergenic.
     * <p>
     * SURF1:NM_003172.4 downstream, 10kb deletion
     * chr9:133_300_001-133_310_000
     *
     * Note that the Jannovar API returns only one transcript for upstream/downstream matches, even though
     * all three SURF1 transcripts start at the same location. In our test we are getting
     * "NM_003172.3", but it is safer to test that we get the expected symbol
     */
    @Test
    public void testDeletionDownstreamIntergenic() {
        SequenceRearrangement surf1Downstream = Deletions.surf1DownstreamIntergenic();
        List<Overlap> overlaps = overlapper.getOverlapList(surf1Downstream);
        assertEquals(1, overlaps.size());
        Overlap overlap = overlaps.get(0);
        assertEquals("SURF1", overlap.getGeneSymbol());
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
        SequenceRearrangement surf1Upstream = Deletions.brca2UpstreamIntergenic();
        List<Overlap> overlaps = overlapper.getOverlapList(surf1Upstream);
        assertEquals(1, overlaps.size());
        Overlap overlap = overlaps.get(0);
        assertEquals("FBN1", overlap.getGeneSymbol());
        assertEquals(UPSTREAM_GENE_VARIANT_500KB, overlap.getOverlapType());
        assertFalse(overlap.overlapsCds());
    }

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
        SequenceRearrangement surf2insertion5utr = Insertions.surf2InsertionIn5UTR();
        List<Overlap> overlaps = overlapper.getOverlapList(surf2insertion5utr);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(SINGLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
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
        SequenceRearrangement surf2insertion3utr = Insertions.surf1InsertionIn3UTR();
        List<Overlap> overlaps = overlapper.getOverlapList(surf2insertion3utr);
        assertEquals(3, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_003172.3","NM_001280787.1", "XM_011518942.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(SINGLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
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
        SequenceRearrangement surf2insertionExon4 = Insertions.surf2Exon4();
        List<Overlap> overlaps = overlapper.getOverlapList(surf2insertionExon4);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
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
     *
     * INTRONIC, NON-CDS
     */
    @Test
    public void testInsertionInIntron3() {
        SequenceRearrangement surf2insertionIntron3 = Insertions.surf2Intron3();
        List<Overlap> overlaps = overlapper.getOverlapList(surf2insertionIntron3);
        assertEquals(2, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_017503.4", "NM_001278928.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            assertEquals(INTRONIC, o.getOverlapType());
            assertFalse(o.overlapsCds());
        }
    }



    @Test
    public void testGetIntronicDistance() {
        SequenceRearrangement surf2insertionIntron3 = Insertions.surf2Intron3();
        List<Overlap> overlaps = overlapper.getOverlapList(surf2insertionIntron3);
        Overlap overlap = overlaps.get(0);
        // there are 249 bases between the deletion and the downstream exon, not including deletion or exonic regions
        // there are 1186 bases between the deletion and the upstream exon, not including deletion or exonic regions
        assertEquals("NM_017503.4", overlap.getAccession());
        assertEquals(249, overlap.getDistance());
    }

    /**
     * Translocation where one CDS is disrupted and the other is not
     * <p>
     * left mate, SURF2:NM_017503.5 intron 3 (disrupted CDS)
     * chr9:133_359_000 (+)
     * right mate, upstream from BRCA2 (not disrupted)
     * chr13:32_300_000 (+)
     *
     * TODO discuss behavior
     * The SURF2 breakend is in an intron of SURF2, but this will clearly disrupt the CDS.
     * I think it is ok that the overlap objects do not know about this, we will
     * do this in the SvPrioritizer class.
     */
    @Test
    public void testTranslocationWhereOneCdsIsDisruptedAndTheOtherIsNot() {
        SequenceRearrangement translocation = translocationWhereOneCdsIsDisruptedAndTheOtherIsNot();
        List<Overlap> overlaps = overlapper.getOverlapList(translocation);
        assertEquals(2, overlaps.size());
        // The following do not work because the breakend is in an intron of SURF2
        //Overlap codingOverlap = overlaps.stream().filter(Overlap::overlapsCds).findFirst().orElseThrow();
        //assertEquals("SURF2", codingOverlap.getGeneSymbol());
    }

}