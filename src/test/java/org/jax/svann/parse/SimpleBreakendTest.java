package org.jax.svann.parse;

import org.jax.svann.ToyCoordinateTestBase;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

public class SimpleBreakendTest extends ToyCoordinateTestBase {

    private static Contig CONTIG;

    private SimpleBreakend instance;

    @BeforeAll
    public static void beforeAll() {
        // contig with length 20
        CONTIG = TOY_ASSEMBLY.getContigByName("ctg2").get();
    }

    @BeforeEach
    public void setUp() {
        instance = SimpleBreakend.preciseWithNoRef(CONTIG, 5, Strand.FWD, "Johnny");
    }

    @Test
    public void withStrand() {
        Breakend breakend = instance.withStrand(Strand.FWD);
        assertThat(breakend, is(sameInstance(instance)));

        breakend = instance.withStrand(Strand.REV);
        assertThat(breakend.getId(), is("Johnny"));
        assertThat(breakend.getPosition(), is(16));
        assertThat(breakend.getStrand(), is(Strand.REV));
    }

    @Test
    public void others() {
        assertThat(instance.getContig(), is(sameInstance(CONTIG)));
    }
}