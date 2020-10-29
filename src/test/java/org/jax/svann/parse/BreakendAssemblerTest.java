package org.jax.svann.parse;

import org.jax.svann.TestBase;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class BreakendAssemblerTest extends TestBase {

    private static Contig CHR2, CHR13, CHR17;
    private BreakendAssembler assembler;

    @BeforeAll
    public static void beforeAll() throws Exception {
        CHR2 = GENOME_ASSEMBLY.getContigByName("chr2").orElseThrow();
        CHR13 = GENOME_ASSEMBLY.getContigByName("chr13").orElseThrow();
        CHR17 = GENOME_ASSEMBLY.getContigByName("chr17").orElseThrow();
    }

    @BeforeEach
    public void setUp() {
        assembler = new BreakendAssembler();
    }

    @Test
    public void assemble_usingEventIds() {
        final BreakendRecord bnd_W = new BreakendRecord(ChromosomalPosition.of(CHR2, Position.precise(321681), Strand.FWD),
                "bnd_W", "TRA0", "bnd_Y", "G", "G]17:198982]");
        final BreakendRecord bnd_Y = new BreakendRecord(ChromosomalPosition.of(CHR17, Position.precise(198982), Strand.FWD),
                "bnd_Y", "TRA0", "bnd_W", "A", "A]2:321681]");

        final BreakendRecord bnd_V = new BreakendRecord(ChromosomalPosition.of(CHR2, Position.precise(321682), Strand.FWD),
                "bnd_V", "TRA1", "bnd_U", "T", "]13:123456]T");
        final BreakendRecord bnd_U = new BreakendRecord(ChromosomalPosition.of(CHR13, Position.precise(123456), Strand.FWD),
                "bnd_U", "TRA1", "bnd_V", "C", "C[2:321682[");

        final BreakendRecord bnd_X = new BreakendRecord(ChromosomalPosition.of(CHR13, Position.precise(198982), Strand.FWD),
                "bnd_X", "TRA2", "bnd_Z", "A", "[17:198983[A");
        final BreakendRecord bnd_Z = new BreakendRecord(ChromosomalPosition.of(CHR17, Position.precise(198983), Strand.FWD),
                "bnd_Z", "TRA2", "bnd_X", "C", "[13:123457[C");

        final List<SequenceRearrangement> rearrangements = assembler.assemble(Set.of(bnd_W, bnd_Y, bnd_V, bnd_U, bnd_X, bnd_Z));
        rearrangements.forEach(System.err::println);
    }

    @Test
    public void assemble_withoutEventIds() {
        final BreakendRecord bnd_W = new BreakendRecord(ChromosomalPosition.of(CHR2, Position.precise(321681), Strand.FWD),
                "bnd_W", null, "bnd_Y", "G", "G]17:198982]");
        final BreakendRecord bnd_Y = new BreakendRecord(ChromosomalPosition.of(CHR17, Position.precise(198982), Strand.FWD),
                "bnd_Y", null, "bnd_W", "A", "A]2:321681]");

        final BreakendRecord bnd_V = new BreakendRecord(ChromosomalPosition.of(CHR2, Position.precise(321682), Strand.FWD),
                "bnd_V", null, "bnd_U", "T", "]13:123456]T");
        final BreakendRecord bnd_U = new BreakendRecord(ChromosomalPosition.of(CHR13, Position.precise(123456), Strand.FWD),
                "bnd_U", null, "bnd_V", "C", "C[2:321682[");

        final BreakendRecord bnd_X = new BreakendRecord(ChromosomalPosition.of(CHR13, Position.precise(198982), Strand.FWD),
                "bnd_X", null, "bnd_Z", "A", "[17:198983[A");
        final BreakendRecord bnd_Z = new BreakendRecord(ChromosomalPosition.of(CHR17, Position.precise(198983), Strand.FWD),
                "bnd_Z", null, "bnd_X", "C", "[13:123457[C");

        final List<SequenceRearrangement> rearrangements = assembler.assemble(Set.of(bnd_W, bnd_Y, bnd_V, bnd_U, bnd_X, bnd_Z));
        assertThat(rearrangements, hasSize(3));

        // not testing anything else now, as these are being tested below
    }

    /**
     * Test assembling situation depicted using mates `W` and `Y` on VCF4.3 | Section 5.4 | Figure 2.
     */
    @Test
    public void assembleBreakendRecords_WY() {
         BreakendRecord bnd_W = new BreakendRecord(ChromosomalPosition.of(CHR2, Position.precise(321681), Strand.FWD),
                "bnd_W", "TRA0", "bnd_Y", "G", "G]17:198982]");
         BreakendRecord bnd_Y = new BreakendRecord(ChromosomalPosition.of(CHR17, Position.precise(198982), Strand.FWD),
                "bnd_Y", "TRA0", "bnd_W", "A", "A]2:321681]");
        Optional<SequenceRearrangement> rearrangementOpt = BreakendAssembler.assembleBreakendRecords(bnd_W, bnd_Y);
        assertThat(rearrangementOpt.isPresent(), is(true));

        SequenceRearrangement rearrangement = rearrangementOpt.get();
        assertThat(rearrangement.getType(), is(SvType.TRANSLOCATION));
        assertThat(rearrangement.getStrand(), is(Strand.FWD)); // W is on FWD strand

        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        assertThat(adjacencies, hasSize(1));

        Adjacency adjacency = adjacencies.get(0);
        assertThat(adjacency.getStrand(), is(Strand.FWD)); // again, W is on FWD strand

        Breakend left = adjacency.getLeft(); // W
        assertThat(left.getId(), is("bnd_W"));
        assertThat(left.getContig().getPrimaryName(), is("2"));
        assertThat(left.getBegin(), is(321681));
        assertThat(left.getStrand(), is(Strand.FWD));
        assertThat(left.getRef(), is("G"));

        Breakend right = adjacency.getRight(); // Y
        assertThat(right.getId(), is("bnd_Y"));
        assertThat(right.getContig().getPrimaryName(), is("17"));
        assertThat(right.getBegin(), is(83058460)); // 83257441 - 198982 + 1 (chr17 length is 83,257,441)
        assertThat(right.getStrand(), is(Strand.REV));
        assertThat(right.getRef(), is("T"));
    }

    /**
     * Test assembling situation depicted using mates `U` and `V` on VCF4.3 | Section 5.4 | Figure 2.
     */
    @Test
    public void assembleBreakendRecords_UV() {
        BreakendRecord bnd_U = new BreakendRecord(ChromosomalPosition.of(CHR13, Position.precise(123456), Strand.FWD),
                "bnd_U", "TRA1", "bnd_V", "C", "C[2:321682[");
        BreakendRecord bnd_V = new BreakendRecord(ChromosomalPosition.of(CHR2, Position.precise(321682), Strand.FWD),
                "bnd_V", "TRA1", "bnd_U", "T", "]13:123456]T");

        Optional<SequenceRearrangement> rearrangementOpt = BreakendAssembler.assembleBreakendRecords(bnd_U, bnd_V);
        assertThat(rearrangementOpt.isPresent(), is(true));

        SequenceRearrangement rearrangement = rearrangementOpt.get();
        assertThat(rearrangement.getType(), is(SvType.TRANSLOCATION));
        assertThat(rearrangement.getStrand(), is(Strand.FWD)); // U is on FWD strand

        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        assertThat(adjacencies, hasSize(1));

        Adjacency adjacency = adjacencies.get(0);
        assertThat(adjacency.getStrand(), is(Strand.FWD)); // again, U is on FWD strand

        Breakend left = adjacency.getLeft(); // U
        assertThat(left.getId(), is("bnd_U"));
        assertThat(left.getContig().getPrimaryName(), is("13"));
        assertThat(left.getBegin(), is(123456));
        assertThat(left.getStrand(), is(Strand.FWD));
        assertThat(left.getRef(), is("C"));

        Breakend right = adjacency.getRight(); // V
        assertThat(right.getId(), is("bnd_V"));
        assertThat(right.getContig().getPrimaryName(), is("2"));
        assertThat(right.getBegin(), is(321682));
        assertThat(right.getStrand(), is(Strand.FWD));
        assertThat(right.getRef(), is("T"));
    }

    /**
     * Test assembling situation depicted using mates `X` and `Z` on VCF4.3 | Section 5.4 | Figure 2.
     */
    @Test
    public void assembleBreakendRecords_XZ() {
        BreakendRecord bnd_X = new BreakendRecord(ChromosomalPosition.of(CHR13, Position.precise(198982), Strand.FWD),
                "bnd_X", "TRA2", "bnd_Z", "A", "[17:198983[A");
        BreakendRecord bnd_Z = new BreakendRecord(ChromosomalPosition.of(CHR17, Position.precise(198983), Strand.FWD),
                "bnd_Z", "TRA2", "bnd_X", "C", "[13:123457[C");

        Optional<SequenceRearrangement> rearrangementOpt = BreakendAssembler.assembleBreakendRecords(bnd_X, bnd_Z);
        assertThat(rearrangementOpt.isPresent(), is(true));

        SequenceRearrangement rearrangement = rearrangementOpt.get();
        assertThat(rearrangement.getType(), is(SvType.TRANSLOCATION));
        assertThat(rearrangement.getStrand(), is(Strand.REV)); // X is on REV strand

        List<Adjacency> adjacencies = rearrangement.getAdjacencies();
        assertThat(adjacencies, hasSize(1));

        Adjacency adjacency = adjacencies.get(0);
        assertThat(adjacency.getStrand(), is(Strand.REV)); // again, X is on REV strand

        Breakend left = adjacency.getLeft(); // X
        assertThat(left.getId(), is("bnd_X"));
        assertThat(left.getContig().getPrimaryName(), is("13"));
        assertThat(left.getBegin(), is(114165347)); // 114364328 - 198982 + 1 (chr13 length is 114,364,328)
        assertThat(left.getStrand(), is(Strand.REV));
        assertThat(left.getRef(), is("T"));

        Breakend right = adjacency.getRight(); // Z
        assertThat(right.getId(), is("bnd_Z"));
        assertThat(right.getContig().getPrimaryName(), is("17"));
        assertThat(right.getBegin(), is(198983));
        assertThat(right.getStrand(), is(Strand.FWD));
        assertThat(right.getRef(), is("C"));
    }

    @Test
    public void assembleBreakendRecords_withInsertedSequence() {
        BreakendRecord bnd_U = new BreakendRecord(ChromosomalPosition.of(CHR13, Position.precise(123456), Strand.FWD),
                "bnd_U", "TRA1", "bnd_V", "C", "CAGTNNNNNCA[2:321682[");
        BreakendRecord bnd_V = new BreakendRecord(ChromosomalPosition.of(CHR2, Position.precise(321682), Strand.FWD),
                "bnd_V", "TRA1", "bnd_U", "T", "]13:123456]AGTNNNNNCAT");

        Optional<SequenceRearrangement> rearrangementOpt = BreakendAssembler.assembleBreakendRecords(bnd_U, bnd_V);
        assertThat(rearrangementOpt.isPresent(), is(true));

        SequenceRearrangement rearrangement = rearrangementOpt.get();
        Adjacency adjacency = rearrangement.getAdjacencies().get(0);
        assertThat(adjacency.getStrand(), is(Strand.FWD)); // again, U is on FWD strand
        assertThat(adjacency.getInserted(), is("CAGTNNNNNCA".getBytes(StandardCharsets.US_ASCII)));

        // now let's do that the other way around, submitting `bnd_V` as the 1st breakend
        rearrangementOpt = BreakendAssembler.assembleBreakendRecords(bnd_V, bnd_U);
        assertThat(rearrangementOpt.isPresent(), is(true));

        rearrangement = rearrangementOpt.get();
        adjacency = rearrangement.getAdjacencies().get(0);
        assertThat(adjacency.getStrand(), is(Strand.REV)); //  V is on FWD strand
        assertThat(adjacency.getInserted(), is("ATGNNNNNACT".getBytes(StandardCharsets.US_ASCII)));

    }
}