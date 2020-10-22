package org.jax.svann.reference;

/**
 * Adjacency ties together 2 breakends.
 */
public class Adjacency {

    private final BreakendCoordinate a, b;

    public Adjacency(BreakendCoordinate a, BreakendCoordinate b) {
        this.a = a;
        this.b = b;
    }

    public BreakendCoordinate mateA() {
        return a;
    }

    public BreakendCoordinate mateB() {
        return b;
    }

}
