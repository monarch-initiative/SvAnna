package org.jax.svann.parse;

import org.jax.svann.reference.SequenceRearrangement;

import java.util.Collection;
import java.util.List;

public interface BreakendAssembler<T extends SequenceRearrangement> {

    List<T> assemble(Collection<BreakendRecord> breakends);
}
