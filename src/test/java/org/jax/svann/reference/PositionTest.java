package org.jax.svann.reference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Deprecated
@Disabled
public class PositionTest {

    private Position precise, imprecise;

    @BeforeEach
    public void setUp() {
        precise = Position.precise(101);
        imprecise = Position.imprecise(101, ConfidenceInterval.of(20, 10));
    }

    @Test
    public void properties() {
        assertThat(precise.getPos(), is(101));
        assertThat(precise.getConfidenceInterval(), is(ConfidenceInterval.precise()));

        assertThat(imprecise.getPos(), is(101));
        assertThat(imprecise.getConfidenceInterval(), is(ConfidenceInterval.of(20, 10)));
    }

    @Test
    public void comparePositionsWithDifferentCIs() {
        // precise is higher/better than imprecise
        assertThat(precise.compareTo(imprecise), is(1));
    }

    @ParameterizedTest
    @CsvSource({
            "1,2,-1",
            "2,2,0",
            "3,2,1",
    })
    public void comparePrecisePositions(int leftPos, int rightPos, int expected) {
        final Position left = Position.precise(leftPos);
        final Position right = Position.precise(rightPos);

        assertThat(left.compareTo(right), is(expected));
    }

    @Test
    public void isPrecise() {
        assertThat(precise.isPrecise(), is(true));
        assertThat(imprecise.isPrecise(), is(false));
    }

    @Test
    public void errorThrownWhenInvalidInput() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Position.precise(0, CoordinateSystem.ONE_BASED));
        assertThat(ex.getMessage(), is("One-based position `0` cannot be non-positive"));

        ex = assertThrows(IllegalArgumentException.class, () -> Position.precise(-1, CoordinateSystem.ZERO_BASED));
        assertThat(ex.getMessage(), is("One-based position `0` cannot be non-positive"));
    }
}