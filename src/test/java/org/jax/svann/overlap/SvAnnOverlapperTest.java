package org.jax.svann.overlap;

import org.jax.svann.TestBase;
import org.jax.svann.parse.TestVariants.Deletions;
import org.jax.svann.parse.TestVariants.Insertions;
import org.jax.svann.parse.TestVariants.Inversions;
import org.jax.svann.reference.SequenceRearrangement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jax.svann.overlap.OverlapType.*;
import static org.junit.jupiter.api.Assertions.*;

public class SvAnnOverlapperTest extends TestBase {

    private SvAnnOverlapper overlapper;

    @BeforeEach
    public void setUp() {
        overlapper = new SvAnnOverlapper(TX_SERVICE.getChromosomeMap());
    }

    @Test
    public void testGetIntronicDistance() {
        SequenceRearrangement surf2insertionIntron3 = Deletions.surf2WithinAnIntron();
        List<Overlap> overlaps = overlapper.getOverlapList(surf2insertionIntron3);

        Overlap overlap = overlaps.get(0);
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
     * We expect three overlapping transcripts for the SURF1 two-exon deletion
     */
    @Test
    public void testSurf1TwoExonDeletion() {
        SequenceRearrangement twoExonSurf1 = Deletions.surf1TwoExon_exons_6_and_7();
        List<Overlap> overlaps = overlapper.getOverlapList(twoExonSurf1);

        assertEquals(3, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_003172.3", "NM_001280787.1", "XM_011518942.1");
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
     */
    @Test
    public void testTwoTranscriptDeletion() {
        SequenceRearrangement surf1and2deletion = Deletions.surf1Surf2oneEntireTranscriptAndPartOfAnother();
        List<Overlap> overlaps = overlapper.getOverlapList(surf1and2deletion);
        assertEquals(5, overlaps.size());
        Set<String> expectedAccessionNumbers =
                Set.of("NM_017503.4", "NM_001278928.1", "NM_003172.3", "NM_001280787.1", "XM_011518942.1");
        Set<String> observedAccessionNumbers = overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet());
        assertEquals(expectedAccessionNumbers, observedAccessionNumbers);
        for (var o : overlaps) {
            if (o.getGeneSymbol().equals("SURF1")) {
                assertEquals(TRANSCRIPT_CONTAINED_IN_SV, o.getOverlapType());
            } else if (o.getGeneSymbol().equals("SURF2")) {
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
     * <p>
     * The deletion is thus EXONIC but NOT CDS
     */
    @Test
    public void testDeletionIn5UTR() {
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
     * <p>
     * The deletion is thus EXONIC but NOT CDS
     */

    @Test
    public void testDeletionIn3UTR() {
        SequenceRearrangement surf1DeletionWithinIntron = Deletions.surf1In3UTR();
        List<Overlap> overlaps = overlapper.getOverlapList(surf1DeletionWithinIntron);
        assertEquals(3, overlaps.size());
        Set<String> expectedAccessionNumbers = Set.of("NM_003172.3", "NM_001280787.1", "XM_011518942.1");
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
     * <p>
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
        Set<String> expectedAccessionNumbers = Set.of("NM_003172.3", "NM_001280787.1", "XM_011518942.1");
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
     * <p>
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
        SequenceRearrangement gckExonic = Inversions.gckExonic();
        List<Overlap> overlaps = overlapper.getOverlapList(gckExonic);

        assertEquals(4, overlaps.size());
        assertThat(overlaps.stream().map(Overlap::getAccession).collect(Collectors.toSet()), hasItems("NM_000162.4", "NM_033507.2", "NM_033508.2", "NM_001354800.1"));
        for (var o : overlaps) {
            assertEquals(SINGLE_EXON_IN_TRANSCRIPT, o.getOverlapType());
            assertTrue(o.overlapsCds());
        }
    }

}