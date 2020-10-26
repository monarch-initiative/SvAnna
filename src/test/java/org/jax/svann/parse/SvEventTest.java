package org.jax.svann.parse;

import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SvEventTest {

    private static Contig CONTIG;

    private SvEvent preciseEvent, impreciseEvent;

    @BeforeAll
    public static void beforeAll() {
        CONTIG = new SimpleContig(1, "contig", 1000);
    }

    @BeforeEach
    public void setUp() {
        preciseEvent = SvEvent.of(CONTIG,
                Position.precise(101, CoordinateSystem.ONE_BASED),
                Position.precise(200, CoordinateSystem.ONE_BASED),
                SvType.DELETION,
                Strand.FWD);
        impreciseEvent = SvEvent.of(CONTIG,
                Position.imprecise(101, ConfidenceInterval.of(20, 10), CoordinateSystem.ONE_BASED),
                Position.imprecise(200, ConfidenceInterval.of(50, 30), CoordinateSystem.ONE_BASED),
                SvType.DELETION,
                Strand.FWD);
    }

    @Test
    public void preciseEventWithStrand() {
        SvEvent onStrand = preciseEvent.withStrand(Strand.FWD);
        assertThat(onStrand, is(sameInstance(preciseEvent)));

        onStrand = preciseEvent.withStrand(Strand.REV);
        assertThat(onStrand.getBeginPosition(), is(Position.precise(801)));
        assertThat(onStrand.getEndPosition(), is(Position.precise(900)));
    }

    @Test
    public void impreciseEventWithStrand() {
        SvEvent onStrand = impreciseEvent.withStrand(Strand.FWD);
        assertThat(onStrand, is(sameInstance(impreciseEvent)));

        onStrand = impreciseEvent.withStrand(Strand.REV);
        assertThat(onStrand.getBeginPosition(), is(Position.imprecise(801, ConfidenceInterval.of(30, 50))));
        assertThat(onStrand.getEndPosition(), is(Position.imprecise(900, ConfidenceInterval.of(10, 20))));
    }

    @Test
    public void errorThrownWhenSpecifyingInvalidCoordinates() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> SvEvent.of(CONTIG, Position.precise(100), Position.precise(1001), SvType.DELETION, Strand.FWD));
        assertThat(ex.getMessage(), is("End position `1001` past the contig end `1000`"));

        ex = assertThrows(IllegalArgumentException.class, () -> SvEvent.of(CONTIG, Position.precise(101), Position.precise(100), SvType.DELETION, Strand.FWD));
        assertThat(ex.getMessage(), is("Begin position `101` past the end position `100`"));
    }
}