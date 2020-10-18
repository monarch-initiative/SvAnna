package org.jax.l2o.vcf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BndTypeTest {


    @Test
    public void testCase1() {
        String alt = "G[chrX:135935363[";
        assertEquals(BndType.CASE_1, BndType.fromAltString(alt));
    }

    @Test
    public void testCase2() {
        String alt = "C]CM000684.2:10740324]";
        assertEquals(BndType.CASE_2, BndType.fromAltString(alt));
    }

    @Test
    public void testCase3() {
        String alt = "]chrX:135754456]G";
        assertEquals(BndType.CASE_3, BndType.fromAltString(alt));
    }

    @Test
    public void testCase4() {
        String alt = "[CM000686.2:11311414[G";
        assertEquals(BndType.CASE_4, BndType.fromAltString(alt));
    }
}
