package org.jax.svann.overlap;


import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.GenomePosition;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.TestBase;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.transcripts.SvAnnTxModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.jax.svann.parse.TestVariants.Deletions.zbtb48intron1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Test the ability of Overlapper to calculate the correct intron number.
 */
public class IntronOverlapTest extends TestBase {

    private static ReferenceDictionary rdict;
    /**
     * transcript info on forward strand
     */
    private static  TranscriptModel ZBTB48;
    /**
     * transcript info on reverse strand
     */
    private static  TranscriptModel ZNF436;

    private static SvAnnOverlapper overlapper;

    /**
     * Mock a TranscriptModel to be able to test getting the correct intron number

     TranscriptModel​(String accession,
     String geneSymbol,
     GenomeInterval txRegion,
     GenomeInterval cdsRegion,
     com.google.common.collect.ImmutableList<GenomeInterval> exonRegions,
     String sequence,
     String geneID,
     int transcriptSupportLevel,
     boolean hasIndels,
     boolean hasSubstitutions)
     */
    @BeforeAll
    private static void init() {
        rdict = TranscriptModelFactory.rdict();
        ZBTB48 = TranscriptModelFactory.ZBTB48();
        ZNF436 = TranscriptModelFactory.ZNF436();
        overlapper = new SvAnnOverlapper(TX_SERVICE.getChromosomeMap());
    }

    /**
     * ZBTB48 is forward strand. Test a variant in intron 1, i.e.,  between 6_640_196-6_640_600
     * THere are 12 overlapping transcripts, and this is intron 1 in all of them
     */
    @Test
    public void testZBTB48_Intron1() {
        SequenceRearrangement zbtb48intron1 = zbtb48intron1();
        List<Overlap> overlaps = overlapper.getOverlapList(zbtb48intron1);
        assertEquals(12, overlaps.size());
        Overlap olap = overlaps
                .stream()
                .filter(ol -> ol.getTranscriptModel().getAccession().equals("NM_005341.3"))
                .findFirst()
                .orElseThrow();
        assertEquals("ZBTB48/NM_005341.3[intron 1]",olap.getDescription());
    }

}
