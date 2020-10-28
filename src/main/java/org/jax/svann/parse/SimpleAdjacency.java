package org.jax.svann.parse;

import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.Strand;

import java.util.Objects;

class SimpleAdjacency implements Adjacency {

    private static final byte[] EMPTY = new byte[0];
    private final Breakend left;
    private final Breakend right;
    private final byte[] inserted;

    private SimpleAdjacency(Breakend left, Breakend right, byte[] inserted) {
        this.left = left;
        this.right = right;
        this.inserted = inserted;
    }

    /**
     * Make adjacency with no inserted sequence.
     */
    static SimpleAdjacency empty(Breakend left, Breakend right) {
        return new SimpleAdjacency(left, right, EMPTY);
    }

    /**
     * Make adjacency with inserted sequence.
     */
    static SimpleAdjacency withInsertedSequence(Breakend left, Breakend right, byte[] inserted) {
        return new SimpleAdjacency(left, right, inserted);
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
        return inserted;
    }

    @Override
    public Adjacency withStrand(Strand strand) {
        if (getStrand().equals(strand)) {
            return this;
        } else {
            // adjust strand and reverse order
            return new SimpleAdjacency(right.toOppositeStrand(), left.toOppositeStrand(), Utils.reverseComplement(inserted));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleAdjacency that = (SimpleAdjacency) o;
        return Objects.equals(left, that.left) &&
                Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "ADJ[" + left + ", " + right + "]";
    }
}
