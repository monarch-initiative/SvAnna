package org.jax.svann.parse;

import org.jax.svann.ToyCoordinateTestBase;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class SimpleAdjacencyTest extends ToyCoordinateTestBase {
    private static final Charset CHARSET = StandardCharsets.US_ASCII;

    private static Breakend LEFT, RIGHT;

    /**
     * This adjacency represents deletion of 7 bases from the toy contig
     */
    private SimpleAdjacency instance;

    @BeforeAll
    public static void beforeAll() {
        // contig with length 20
        Contig CONTIG = TOY_ASSEMBLY.getContigByName("ctg2").orElseThrow();
        LEFT = SimpleBreakend.precise(CONTIG, 6, Strand.FWD, "LEFT");
        RIGHT = SimpleBreakend.precise(CONTIG, 12, Strand.FWD, "RIGHT");
    }

    @BeforeEach
    public void setUp() {
        instance = SimpleAdjacency.withInsertedSequence(LEFT, RIGHT, "ACGT".getBytes(CHARSET));
    }

    @Test
    public void withStrand() {
        // no adjustment when asking to flip to the current strand
        Adjacency adjacency = instance.withStrand(Strand.FWD);
        assertThat(adjacency, is(sameInstance(instance)));


        adjacency = instance.withStrand(Strand.REV);
        assertThat(adjacency.getStrand(), is(Strand.REV));
        String inserted = CHARSET.decode(ByteBuffer.wrap(adjacency.getInserted())).toString();
        assertThat(inserted, is("ACGT"));

        Breakend left = adjacency.getStart();
        assertThat(left.getId(), is("RIGHT"));
        assertThat(left.getPosition(), is(9));

        Breakend right = adjacency.getEnd();
        assertThat(right.getId(), is("LEFT"));
        assertThat(right.getPosition(), is(15));
    }
}