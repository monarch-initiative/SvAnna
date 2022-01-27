package org.jax.svanna.io.service.jannovar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class IntervalTest {
    Interval<String> interval;

    @BeforeEach
    public void setUp() {
        interval = new Interval<>(1, 10, "x", 13);
    }

    @Test
    public void testValues() {
        assertEquals(1, interval.getBegin());
        assertEquals(10, interval.getEnd());
        assertEquals(13, interval.getMaxEnd());
        assertEquals("x", interval.getValue());
    }

    @Test
    public void testAllLeftOf() {
        assertFalse(interval.allLeftOf(10));
        assertFalse(interval.allLeftOf(12));
        assertTrue(interval.allLeftOf(13));
        assertTrue(interval.allLeftOf(14));
    }

    @Test
    public void testCompareTo() {
        assertTrue(interval.compareTo(new Interval<>(0, 10, "y", 10)) > 0);
        assertTrue(interval.compareTo(new Interval<>(1, 9, "y", 10)) > 0);
        assertTrue(interval.compareTo(new Interval<>(1, 10, "y", 10)) == 0);
        assertTrue(interval.compareTo(new Interval<>(1, 11, "y", 10)) < 0);
        assertTrue(interval.compareTo(new Interval<>(2, 10, "y", 10)) < 0);
    }

    @Test
    public void testContains() {
        assertFalse(interval.contains(0));
        assertTrue(interval.contains(1));
        assertTrue(interval.contains(2));
        assertTrue(interval.contains(9));
        assertFalse(interval.contains(10));
    }

    @Test
    public void testIsLeftOf() {
        assertFalse(interval.isLeftOf(8));
        assertFalse(interval.isLeftOf(9));
        assertTrue(interval.isLeftOf(10));
        assertTrue(interval.isLeftOf(11));
    }

    @Test
    public void testIsRightOf() {
        assertTrue(interval.isRightOf(-1));
        assertTrue(interval.isRightOf(0));
        assertFalse(interval.isRightOf(1));
        assertFalse(interval.isRightOf(2));
    }

    @Test
    public void overlapsWith() {
        assertTrue(interval.overlapsWith(0, 2));
        assertTrue(interval.overlapsWith(0, 3));
        assertTrue(interval.overlapsWith(0, 20));
        assertTrue(interval.overlapsWith(9, 10));

        assertFalse(interval.overlapsWith(0, 1));
        assertFalse(interval.overlapsWith(10, 11));
    }

}