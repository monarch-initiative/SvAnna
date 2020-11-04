package org.jax.svann.parse;

import org.jax.svann.ToyCoordinateTestBase;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SimpleSequenceRearrangementTest extends ToyCoordinateTestBase {

    /**
     * This rearrangement represents an inversion of 10 bp-long segment on the contig
     */
    private SimpleSequenceRearrangement instance;


    @BeforeEach
    public void setUp() {
        // this contig has 30 bp
        Contig CONTIG = TOY_ASSEMBLY.getContigByName("ctg1").get();

        Breakend alphaLeft = SimpleBreakend.preciseWithNoRef(CONTIG, 10, Strand.FWD, "alphaLeft");
        Breakend alphaRight = SimpleBreakend.preciseWithNoRef(CONTIG, 12, Strand.REV, "alphaRight");
        Adjacency alpha = SimpleAdjacency.empty(alphaLeft, alphaRight);

        Breakend betaLeft = SimpleBreakend.preciseWithNoRef(CONTIG, 20, Strand.REV, "betaLeft");
        Breakend betaRight = SimpleBreakend.preciseWithNoRef(CONTIG, 20, Strand.FWD, "betaRight");
        Adjacency beta = SimpleAdjacency.empty(betaLeft, betaRight);

        instance = SimpleSequenceRearrangement.of(SvType.INVERSION, alpha, beta);
    }

    @Test
    public void withStrand() {
        /*
        We test flipping an inversion rearrangement, which consists of 2 adjacencies.
        This is one of the quite complicated situations that can happen
        */

        // arrange
        SequenceRearrangement rearrangement = instance.withStrand(Strand.FWD);
        assertThat(rearrangement, is(sameInstance(instance)));

        // act
        rearrangement = instance.withStrand(Strand.REV);

        // assert
        assertThat(rearrangement.getLeftmostStrand(), is(Strand.REV));
        assertThat(rearrangement.getType(), is(SvType.INVERSION));

        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        assertThat(adjacencies, hasSize(2));

        Adjacency alpha = adjacencies.get(0);
        assertThat(alpha.getStrand(), is(Strand.REV));

        Breakend alphaLeft = alpha.getStart();
        assertThat(alphaLeft.getPosition(), is(11));
        assertThat(alphaLeft.getStrand(), is(Strand.REV));
        assertThat(alphaLeft.getId(), is("betaRight"));

        Breakend alphaRight = alpha.getEnd();
        assertThat(alphaRight.getPosition(), is(11));
        assertThat(alphaRight.getStrand(), is(Strand.FWD));
        assertThat(alphaRight.getId(), is("betaLeft"));

        Adjacency beta = adjacencies.get(1);
        Breakend betaLeft = beta.getStart();
        assertThat(betaLeft.getPosition(), is(19));
        assertThat(betaLeft.getStrand(), is(Strand.FWD));
        assertThat(betaLeft.getId(), is("alphaRight"));

        Breakend betaRight = beta.getEnd();
        assertThat(betaRight.getPosition(), is(21));
        assertThat(betaRight.getStrand(), is(Strand.REV));
        assertThat(betaRight.getId(), is("alphaLeft"));
    }

    @Test
    public void getLeftmostStrand() {
        assertThat(instance.getLeftmostStrand(), is(Strand.FWD));
    }

    @Test
    public void getLeftmostPosition() {
        assertThat(instance.getLeftmostPosition(), is(10));
    }

    @Test
    public void getRightmostStrand() {
        assertThat(instance.getRightmostStrand(), is(Strand.FWD));
    }

    @Test
    public void getRightmostPosition() {
        assertThat(instance.getRightmostPosition(), is(20));
    }

    @Test
    public void getRegions() {
        List<CoordinatePair> regions = instance.getRegions();
        // TODO: 5. 11. 2020 implement
        regions.forEach(System.err::println);
    }
}