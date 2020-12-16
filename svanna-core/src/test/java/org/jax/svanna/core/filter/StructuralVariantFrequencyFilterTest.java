package org.jax.svanna.core.filter;

import org.jax.svanna.core.TestDataConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.variant.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

@SpringBootTest(classes = TestDataConfig.class)
public class StructuralVariantFrequencyFilterTest {

    private static final double TOLERANCE = 5E-9;

//    private StructuralVariantFrequencyFilter filter;


    @ParameterizedTest
    @CsvSource({
            "10, 20, POSITIVE,     5,  15, POSITIVE,     .5",
            "10, 20, POSITIVE,     15, 25, POSITIVE,     .5",
            "10, 20, POSITIVE,     0,  30, POSITIVE,     .33333333",
            "10, 20, POSITIVE,     15, 18, POSITIVE,     .3",
            "10, 20, POSITIVE,     15, 20, POSITIVE,     .5",
            "10, 20, POSITIVE,     10, 15, POSITIVE,     .5",

            "10, 20, POSITIVE,     10, 20, POSITIVE,     1.",
            "10, 20, POSITIVE,      9, 10, POSITIVE,     0.",
            "10, 20, POSITIVE,     20, 21, POSITIVE,     0.",

            "10, 20, POSITIVE,     75, 85, NEGATIVE,     .5",
    })
    public void reciprocalOverlap(int leftStart, int leftEnd, Strand leftStrand,
                                  int rightStart, int rightEnd, Strand rightStrand,
                                  float expected) {
        Contig contig = Contig.of(1, "1", SequenceRole.ASSEMBLED_MOLECULE, 100, "", "", "");

        GenomicRegion left = GenomicRegion.of(contig, leftStrand, CoordinateSystem.ZERO_BASED, Position.of(leftStart), Position.of(leftEnd));
        GenomicRegion right = GenomicRegion.of(contig, rightStrand, CoordinateSystem.ZERO_BASED, Position.of(rightStart), Position.of(rightEnd));

        assertThat((double) StructuralVariantFrequencyFilter.reciprocalOverlap(left, right), closeTo(expected, TOLERANCE));
        assertThat((double) StructuralVariantFrequencyFilter.reciprocalOverlap(right, left), closeTo(expected, TOLERANCE));
    }
}