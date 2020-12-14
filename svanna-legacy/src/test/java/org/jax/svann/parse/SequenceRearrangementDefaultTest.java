package org.jax.svann.parse;

import org.jax.svann.ToyCoordinateTestBase;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SequenceRearrangementDefaultTest extends ToyCoordinateTestBase {

    /**
     * This rearrangement represents an inversion of 10 bp-long segment on the contig
     */
    private SequenceRearrangementDefault inversion;
    private SequenceRearrangementDefault deletion;
    private SequenceRearrangementDefault translocation;
    private SequenceRearrangementDefault insertion;


    @BeforeEach
    public void setUp() {

        Contig ctg1 = TOY_ASSEMBLY.getContigByName("ctg1").orElseThrow(); // this contig has 30 bp
        Contig ctg2 = TOY_ASSEMBLY.getContigByName("ctg2").orElseThrow(); // length 20 bp

        // Make INVERSION
        Breakend alphaLeft = BreakendDefault.precise(ctg1, 10, Strand.FWD, "alphaLeft");
        Breakend alphaRight = BreakendDefault.precise(ctg1, 12, Strand.REV, "alphaRight");
        Adjacency alpha = AdjacencyDefault.empty(alphaLeft, alphaRight);

        Breakend betaLeft = BreakendDefault.precise(ctg1, 20, Strand.REV, "betaLeft");
        Breakend betaRight = BreakendDefault.precise(ctg1, 20, Strand.FWD, "betaRight");
        Adjacency beta = AdjacencyDefault.empty(betaLeft, betaRight);
        inversion = SequenceRearrangementDefault.of(SvType.INVERSION, alpha, beta);

        // Make DELETION
        Breakend delRight = BreakendDefault.precise(ctg1, 21, Strand.FWD, "delRight");
        deletion = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(alphaLeft, delRight));

        // Make TRANSLOCATION
        Breakend transloc = BreakendDefault.precise(ctg2, 15, Strand.FWD, "translocation");
        translocation = SequenceRearrangementDefault.of(SvType.TRANSLOCATION, AdjacencyDefault.empty(alphaLeft, transloc));

        // Make INSERTION of 10bp
        Contig insertionContig = new InsertionContig("insertionCtg1", 10);
        Breakend insLeft = BreakendDefault.precise(ctg1, 15, Strand.FWD, "insLeft");
        Breakend insMiddleLeft = BreakendDefault.precise(insertionContig, 1, Strand.FWD, "insMiddleLeft");
        Breakend insMiddleRight = BreakendDefault.precise(insertionContig, insertionContig.getLength(), Strand.FWD, "insMiddleRight");
        Breakend insRight = BreakendDefault.precise(ctg1, 16, Strand.FWD, "insRight");
        insertion = SequenceRearrangementDefault.of(SvType.INSERTION, AdjacencyDefault.empty(insLeft, insMiddleLeft), AdjacencyDefault.empty(insMiddleRight, insRight));
    }

    @Test
    public void withStrand() {
        /*
        We test flipping an inversion rearrangement, which consists of 2 adjacencies.
        This is one of the quite complicated situations that can happen
        */

        // arrange
        SequenceRearrangement rearrangement = inversion.withStrand(Strand.FWD);
        assertThat(rearrangement, is(sameInstance(inversion)));

        // act
        rearrangement = inversion.withStrand(Strand.REV);

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
        assertThat(inversion.getLeftmostStrand(), is(Strand.FWD));
    }

    @Test
    public void getLeftmostPosition() {
        assertThat(inversion.getLeftmostPosition(), is(10));
    }

    @Test
    public void getRightmostStrand() {
        assertThat(inversion.getRightmostStrand(), is(Strand.FWD));
    }

    @Test
    public void getRightmostPosition() {
        assertThat(inversion.getRightmostPosition(), is(20));
    }

    @Test
    public void getRegions_Inversion() {
        List<CoordinatePair> regions = inversion.getRegions();

        assertThat(regions, hasSize(1));
        CoordinatePair pair = regions.get(0);
        GenomicPosition start = pair.getStart();
        assertThat(start.getContigId(), is(1));
        assertThat(start.getPosition(), is(12));
        assertThat(start.getStrand(), is(Strand.REV));

        GenomicPosition end = pair.getEnd();
        assertThat(end.getContigId(), is(1));
        assertThat(end.getPosition(), is(20));
        assertThat(end.getStrand(), is(Strand.REV));
    }

    @Test
    public void getRegions_Insertion() {
        List<CoordinatePair> regions = insertion.getRegions();

        assertThat(regions, hasSize(1));
        CoordinatePair pair = regions.get(0);
        GenomicPosition start = pair.getStart();
        assertThat(start.getContigId(), is(1));
        assertThat(start.getPosition(), is(15));
        assertThat(start.getStrand(), is(Strand.FWD));

        GenomicPosition end = pair.getEnd();
        assertThat(end.getContigId(), is(1));
        assertThat(end.getPosition(), is(16));
        assertThat(end.getStrand(), is(Strand.FWD));
    }

    @Test
    public void getRegions_Deletion() {
        List<CoordinatePair> regions = deletion.getRegions();

        assertThat(regions, hasSize(1));
        CoordinatePair pair = regions.get(0);
        GenomicPosition start = pair.getStart();
        assertThat(start.getContigId(), is(1));
        assertThat(start.getPosition(), is(10));
        assertThat(start.getStrand(), is(Strand.FWD));

        GenomicPosition end = pair.getEnd();
        assertThat(end.getContigId(), is(1));
        assertThat(end.getPosition(), is(21));
        assertThat(end.getStrand(), is(Strand.FWD));
    }

    @Test
    public void getRegions_Translocation() {
        List<CoordinatePair> regions = translocation.getRegions();

        assertThat(regions, hasSize(1));
        CoordinatePair pair = regions.get(0);
        GenomicPosition start = pair.getStart();
        assertThat(start.getContigId(), is(1));
        assertThat(start.getPosition(), is(10));
        assertThat(start.getStrand(), is(Strand.FWD));

        GenomicPosition end = pair.getEnd();
        assertThat(end.getContigId(), is(2));
        assertThat(end.getPosition(), is(15));
        assertThat(end.getStrand(), is(Strand.FWD));
    }
}