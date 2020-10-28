package org.jax.svann.parse;

import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.Strand;

import java.util.Objects;

/**
 * Simple adjacency ties together 2 breakends with no inserted sequence.
 */
class SimpleAdjacency implements Adjacency {

    private static final byte[] EMPTY = new byte[0];

    private final Breakend left, right;

    private SimpleAdjacency(Breakend left, Breakend right) {
        this.left = left;
        this.right = right;
    }

    static Adjacency of(Breakend left, Breakend right) {
        return new SimpleAdjacency(left, right);
    }

    @Override
    public Breakend getLeft() {
        return left;
    }

    @Override
    public Breakend getRight() {
        return right;
    }

    @Override
    public byte[] getInserted() {
        return EMPTY;
    }

    @Override
    public Adjacency withStrand(Strand strand) {
        if (getStrand().equals(strand)) {
            return this;
        } else {
            return new SimpleAdjacency(right.withStrand(strand), left.withStrand(strand));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleAdjacency adjacency = (SimpleAdjacency) o;
        return Objects.equals(left, adjacency.left) &&
                Objects.equals(right, adjacency.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "Adjacency{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }
}
