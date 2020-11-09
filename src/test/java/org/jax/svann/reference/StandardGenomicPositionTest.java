package org.jax.svann.reference;

import org.jax.svann.ToyCoordinateTestBase;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class StandardGenomicPositionTest extends ToyCoordinateTestBase {

    /**
     * Small contig with length 30.
     */
    private static final Contig CTG1 = TOY_ASSEMBLY.getContigById(1).orElseThrow();

    private GenomicPosition position;

    @BeforeEach
    public void setUp() {
        position = StandardGenomicPosition.precise(CTG1, 15, Strand.FWD);
    }

    @ParameterizedTest
    @CsvSource({
            "10,FWD,-5",
            "15,FWD,0",
            "20,FWD,5",
            "21,REV,-5",
            "16,REV,0",
            "11,REV,5"
    })
    public void distanceTo(int pos, Strand strand, int expected) {
        StandardGenomicPosition other = StandardGenomicPosition.precise(CTG1, pos, strand);
        int diff = position.distanceTo(other);

        assertThat(diff, is(expected));
    }


    @ParameterizedTest
    @CsvSource({
            "10,FWD,true",
            "15,FWD,false",
            "20,FWD,false",
            "21,REV,false",
            "16,REV,false",
            "11,REV,true"
    })
    public void isUpstreamOf(int pos, Strand strand, boolean expected) {
        StandardGenomicPosition other = StandardGenomicPosition.precise(CTG1, pos, strand);
        boolean actual = other.isUpstreamOf(position);

        assertThat(actual, is(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "10,FWD,false",
            "15,FWD,false",
            "20,FWD,true",
            "21,REV,true",
            "16,REV,false",
            "11,REV,false"
    })
    public void isDownstreamOf(int pos, Strand strand, boolean expected) {
        StandardGenomicPosition other = StandardGenomicPosition.precise(CTG1, pos, strand);
        boolean actual = other.isDownstreamOf(position);

        assertThat(actual, is(expected));
    }

}