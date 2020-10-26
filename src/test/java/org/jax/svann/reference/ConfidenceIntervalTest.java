package org.jax.svann.reference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConfidenceIntervalTest {

    private ConfidenceInterval precise;

    private ConfidenceInterval imprecise;

    @BeforeEach
    public void setUp() {
        precise = ConfidenceInterval.precise();
        imprecise = ConfidenceInterval.of(10, 20);
    }

    @Test
    public void preciseCiIsAlwaysTheSameInstance() {
        assertThat(precise, is(sameInstance(ConfidenceInterval.precise())));
    }

    @Test
    public void creatingPreciseCiUsingImpreciseMethodReturnsPreciseCi() {
        final ConfidenceInterval ci = ConfidenceInterval.of(0, 0);
        assertThat(ci, is(sameInstance(precise)));
    }

    @Test
    public void toOppositeStrand() {
        assertThat(precise.toOppositeStrand(), is(sameInstance(precise)));

        final ConfidenceInterval oppositeStrand = imprecise.toOppositeStrand();
        assertThat(oppositeStrand.getUpstream(), is(imprecise.getDownstream()));
        assertThat(oppositeStrand.getDownstream(), is(imprecise.getUpstream()));
    }

    @Test
    public void length() {
        assertThat(precise.length(), is(0));
        assertThat(imprecise.length(), is(30));
    }

    @ParameterizedTest
    @CsvSource({
            "10,10,11,10,1", // left is shorter -> more
            "10,10,10,11,1", // left is shorter -> more
            "10,11,11,10,0", // equal size -> equal
            "0,0,0,0,0",     // precise CIs are equal
            "10,10,10,9,-1", // left is longer -> less
            "10,10,9,10,-1"  // left is longer -> less

    })
    public void compareTo(int leftUpstream, int leftDownstream, int rightUpstream, int rightDownstream, int expected) {
        final ConfidenceInterval left = ConfidenceInterval.of(leftUpstream, leftDownstream);
        final ConfidenceInterval right = ConfidenceInterval.of(rightUpstream, rightDownstream);

        assertThat(left.compareTo(right), is(expected));
    }


    @Test
    public void ciToString() {
        assertThat(precise.toString(), is("[0,0]"));
        assertThat(imprecise.toString(), is("[10,20]"));
    }
}