package org.jax.svann.overlap;

import org.jax.svann.reference.SequenceRearrangement;

import java.util.List;

public interface Overlapper {

    List<Overlap> getOverlapList(SequenceRearrangement rearrangement);

}
