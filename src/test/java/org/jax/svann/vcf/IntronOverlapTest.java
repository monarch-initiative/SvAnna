package org.jax.svann.vcf;


import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.reference.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class IntronOverlapTest {

    private static ReferenceDictionary rdict;
    /**
     * transcript info on forward strand
     */
    private static  TranscriptModel ZBTB48;
    /**
     * transcript info on reverse strand
     */
    private static  TranscriptModel ZNF436;

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
    }






    /**
     * ZBTB48 is forward strand. Test a variant in intron 1, i.e.,  between 6_640_196-6_640_600
     */
    @Test
    public void testZBTB48_Intron1() {
        GenomePosition start = new GenomePosition(rdict, Strand.FWD, 1, 6_640_400);
        GenomePosition end = new GenomePosition(rdict, Strand.FWD, 1, 6_640_500);
        int expectedIntronNumber = 1;
        assertEquals(expectedIntronNumber, IntronOverlap.getIntronNumber(ZBTB48, start.getPos(), end.getPos()));
    }

    /**
     * ZBTB48 is forward strand. Test a variant in intron 2, i.e., between 6641359-6642117
     */
    @Test
    public void testIntronicZBTB48_Intron2() {
        GenomePosition start = new GenomePosition(rdict, Strand.FWD, 1, 6_641_400);
        GenomePosition end = new GenomePosition(rdict, Strand.FWD, 1, 6_641_500);
        int expectedIntronNumber = 2;
        assertEquals(expectedIntronNumber, IntronOverlap.getIntronNumber(ZBTB48, start.getPos(), end.getPos()));
    }

    /**
     * ZBTB48 is forward strand. Test a variant in intron 3, i.e., between 6_642_359-6_645_978
     */
    @Test
    public void testIntronicZBTB48_Intron3() {
        GenomePosition start = new GenomePosition(rdict, Strand.FWD, 1, 6_643_200);
        GenomePosition end = new GenomePosition(rdict, Strand.FWD, 1, 6_643_700);
        int expectedIntronNumber = 3;
        assertEquals(expectedIntronNumber, IntronOverlap.getIntronNumber(ZBTB48, start.getPos(), end.getPos()));
    }

    /**
     * ZBTB48 is forward strand. Test a variant in intron 4, i.e., between 6646090-6646754
     */
    @Test
    public void testIntronicZBTB48_Intron4() {
        GenomePosition start = new GenomePosition(rdict, Strand.FWD, 1, 6_646_200);
        GenomePosition end = new GenomePosition(rdict, Strand.FWD, 1, 6_646_700);
        int expectedIntronNumber = 4;
        assertEquals(expectedIntronNumber, IntronOverlap.getIntronNumber(ZBTB48, start.getPos(), end.getPos()));
    }



    /**
     * ZBTB48 is forward strand. Test a variant in intron 10, i.e., between 6_648_815-6_649_340
     */
    @Test
    public void testIntronicZBTB48_Intron10() {
        GenomePosition start = new GenomePosition(rdict, Strand.FWD, 1, 6_648_915);
        GenomePosition end = new GenomePosition(rdict, Strand.FWD, 1, 6_648_925);
        int expectedIntronNumber = 10;
        assertEquals(expectedIntronNumber, IntronOverlap.getIntronNumber(ZBTB48, start.getPos(), end.getPos()));
    }



    /**
     * ZNF436 is reverse strand. Test a variant in intron 1, 23_694_558-23_695_858
     */
    @Test
    public void testZNF436_Intron1() {
        GenomePosition start = new GenomePosition(rdict, Strand.FWD, 1, 23_694_570);
        GenomePosition end = new GenomePosition(rdict, Strand.FWD, 1, 23_694_580);
        int expectedIntronNumber = 1;
        assertEquals(expectedIntronNumber, IntronOverlap.getIntronNumber(ZNF436, start.getPos(), end.getPos()));
    }

    /**
     * ZNF436 is reverse strand. Test a variant in intron 2, 23_693_661- 23_694_465
     *
     */
    @Test
    public void testZNF436_Intron2() {
        GenomePosition start = new GenomePosition(rdict, Strand.FWD, 1, 23_694_161);
        GenomePosition end = new GenomePosition(rdict, Strand.FWD, 1, 23_694_280);
        int expectedIntronNumber = 2;
        assertEquals(expectedIntronNumber, IntronOverlap.getIntronNumber(ZNF436, start.getPos(), end.getPos()));
    }

    /**
     * ZNF436 is reverse strand. Test a variant in intron 1, 23_689_714-23_693_534
     */
    @Test
    public void testZNF436_Intron3() {
        GenomePosition start = new GenomePosition(rdict, Strand.FWD, 1, 23_693_234);
        GenomePosition end = new GenomePosition(rdict, Strand.FWD, 1, 23_693_334);
        int expectedIntronNumber = 3;
        assertEquals(expectedIntronNumber, IntronOverlap.getIntronNumber(ZNF436, start.getPos(), end.getPos()));
    }
}
