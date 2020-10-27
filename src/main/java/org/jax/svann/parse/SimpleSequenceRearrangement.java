package org.jax.svann.parse;

import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;

import java.util.List;

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
        // TODO: 27. 10. 2020 implement
        return null;
    }
}
