package org.jax.svann.parse;

import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.Strand;

import java.util.Arrays;
import java.util.Objects;

class AdjacencyDefault implements Adjacency {

    private static final byte[] EMPTY = new byte[0];
    private final Breakend left;
    private final Breakend right;
    private final byte[] inserted;
    private final int depthOfCoverage;

    private AdjacencyDefault(Breakend left,
                             Breakend right,
                             byte[] inserted,
                             int depthOfCoverage) {
        this.left = left;
        this.right = right;
        this.inserted = inserted;
        if (depthOfCoverage < -1) {
            throw new IllegalArgumentException("Depth of coverage must be non-negative or -1 : " + depthOfCoverage);
        }
        this.depthOfCoverage = depthOfCoverage;
    }

    /**
     * Make adjacency with no inserted sequence.
     */
    static AdjacencyDefault empty(Breakend left, Breakend right) {
        return withInsertedSequence(left, right, EMPTY);
    }

    static AdjacencyDefault emptyWithDepth(Breakend left, Breakend right, int depthOfCoverage) {
        return withInsertedSequenceAndDepth(left, right, EMPTY, depthOfCoverage);
    }

    /**
     * Make adjacency with inserted sequence.
     */
    static AdjacencyDefault withInsertedSequence(Breakend left, Breakend right, byte[] inserted) {
        return withInsertedSequenceAndDepth(left, right, inserted, MISSING_DEPTH_PLACEHOLDER);
    }

    static AdjacencyDefault withInsertedSequenceAndDepth(Breakend left,
                                                         Breakend right,
                                                         byte[] inserted,
                                                         int depthOfCoverage) {
        return new AdjacencyDefault(left, right, inserted, depthOfCoverage);
    }

    @Override
    @Deprecated
    public Breakend getLeft() {
        return left;
    }

    @Override
    public Breakend getStart() {
        return left;
    }

    @Override
    @Deprecated
    public Breakend getRight() {
        return right;
    }

    @Override
    public Breakend getEnd() {
        return right;
    }

    @Override
    public byte[] getInserted() {
        return inserted;
    }

    @Override
    public int depthOfCoverage() {
        return depthOfCoverage;
    }

    @Override
    public Adjacency withStrand(Strand strand) {
        if (getStrand().equals(strand)) {
            return this;
        } else {
            // adjust strand and reverse order
            return new AdjacencyDefault(right.toOppositeStrand(),
                    left.toOppositeStrand(),
                    Utils.reverseComplement(inserted),
                    depthOfCoverage);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdjacencyDefault that = (AdjacencyDefault) o;
        return depthOfCoverage == that.depthOfCoverage &&
                Objects.equals(left, that.left) &&
                Objects.equals(right, that.right) &&
                Arrays.equals(inserted, that.inserted);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(depthOfCoverage, left, right);
        result = 31 * result + Arrays.hashCode(inserted);
        return result;
    }

    @Override
    public String toString() {
        return "ADJ[" + left + ", " + right + "]";
    }
}
