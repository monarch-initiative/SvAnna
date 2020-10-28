package org.jax.svann.parse;

import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.SvType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class SimpleSequenceRearrangement implements SequenceRearrangement {

    private final List<Adjacency> adjacencies;

    private final SvType type;

    SimpleSequenceRearrangement(List<Adjacency> adjacencies, SvType type) {
        this.adjacencies = adjacencies;
        this.type = type;
    }

    @Override
    public List<Adjacency> getAdjacencies() {
        return adjacencies;
    }

    @Override
    public SvType getType() {
        return type;
    }

    @Override
    public SequenceRearrangement withStrand(Strand strand) {
        if (adjacencies.isEmpty() || adjacencies.get(0).getStrand().equals(strand)) {
            return this;
        } else {
            // reverse order of adjacencies, while also flipping the adjacencies to the opposite strand
            List<Adjacency> reversed = new ArrayList<>(adjacencies.size());
            for (int i = adjacencies.size() - 1; i >= 0; i--) {
                reversed.add(adjacencies.get(i).toOppositeStrand());
            }
            return new SimpleSequenceRearrangement(reversed, type);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleSequenceRearrangement that = (SimpleSequenceRearrangement) o;
        return Objects.equals(adjacencies, that.adjacencies) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(adjacencies, type);
    }

    @Override
    public String toString() {
        return "RR(" + type + "): [" + adjacencies + "]";
    }
}
