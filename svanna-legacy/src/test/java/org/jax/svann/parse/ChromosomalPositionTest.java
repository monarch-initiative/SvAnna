package org.jax.svann.parse;

import org.jax.svann.ContigImpl;
import org.jax.svann.reference.ConfidenceInterval;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

public class ChromosomalPositionTest {

    private static Contig CONTIG;

    @BeforeAll
    public static void beforeAll() {
        CONTIG = new ContigImpl(1, "contig", 10);
    }

    private ChromosomalPosition precise, imprecise;

    @BeforeEach
    public void setUp() {
        precise = ChromosomalPosition.precise(CONTIG, 3, Strand.FWD);
        imprecise = ChromosomalPosition.imprecise(CONTIG, 5, ConfidenceInterval.of(1, 2), Strand.FWD);
    }

    @Test
    public void withStrand() {
        // precise
        ChromosomalPosition pos = precise.withStrand(Strand.FWD);
        assertThat(pos, is(sameInstance(precise)));

        pos = precise.withStrand(Strand.REV);
        assertThat(pos.getPosition(), is(8));
        assertThat(pos.getCi(), is(ConfidenceInterval.precise()));
        assertThat(pos.getStrand(), is(Strand.REV));

        // imprecise
        pos = imprecise.withStrand(Strand.FWD);
        assertThat(pos, is(sameInstance(imprecise)));

        pos = imprecise.withStrand(Strand.REV);
        assertThat(pos.getPosition(), is(6));
        assertThat(pos.getCi(), is(ConfidenceInterval.of(2, 1)));
        assertThat(pos.getStrand(), is(Strand.REV));
    }
}