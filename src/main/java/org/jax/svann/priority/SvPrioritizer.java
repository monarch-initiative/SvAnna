package org.jax.svann.priority;

import org.jax.svann.reference.SequenceRearrangement;


public interface SvPrioritizer {

    SvPriority prioritize(SequenceRearrangement rearrangement);

}
