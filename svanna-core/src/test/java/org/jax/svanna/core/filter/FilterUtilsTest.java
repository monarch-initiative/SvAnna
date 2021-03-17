package org.jax.svanna.core.filter;

import org.jax.svanna.core.TestContig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

public class FilterUtilsTest {

    private static final double TOLERANCE = 5E-9;

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
        Contig contig = TestContig.of(1, 100);

        GenomicRegion left = GenomicRegion.of(contig, leftStrand, CoordinateSystem.zeroBased(), Position.of(leftStart), Position.of(leftEnd));
        GenomicRegion right = GenomicRegion.of(contig, rightStrand, CoordinateSystem.zeroBased(), Position.of(rightStart), Position.of(rightEnd));

        assertThat((double) FilterUtils.reciprocalOverlap(left, right), closeTo(expected, TOLERANCE));
        assertThat((double) FilterUtils.reciprocalOverlap(right, left), closeTo(expected, TOLERANCE));
    }

    @ParameterizedTest
    @CsvSource({
            "POSITIVE,  10, 30,     POSITIVE, 20,  40,     .5",
            "POSITIVE,  20, 20,     POSITIVE, 20,  40,     .0",
            "POSITIVE,  20, 21,     POSITIVE, 20,  40,     .05",
            "POSITIVE,  30, 40,     POSITIVE, 20,  40,     .5",
            "POSITIVE,  39, 40,     POSITIVE, 20,  40,     .05",
            "POSITIVE,  20, 40,     POSITIVE, 20,  40,    1.",
            "POSITIVE,  40, 40,     POSITIVE, 20,  40,     .0",
            "POSITIVE,  35, 50,     POSITIVE, 20,  40,     .25",

    })
    public void fractionShared(Strand queryStrand, int queryStart, int queryEnd,
                               Strand targetStrand, int targetStart, int targetEnd,
                               double expected) {
        Contig contig = TestContig.of(1, 100);
        GenomicRegion query = GenomicRegion.of(contig, queryStrand, CoordinateSystem.zeroBased(), Position.of(queryStart), Position.of(queryEnd));
        GenomicRegion target = GenomicRegion.of(contig, targetStrand, CoordinateSystem.zeroBased(), Position.of(targetStart), Position.of(targetEnd));

        assertThat((double) FilterUtils.fractionShared(query, target), closeTo(expected, TOLERANCE));
    }
}