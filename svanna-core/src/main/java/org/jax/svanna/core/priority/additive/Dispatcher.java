package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.GenomicVariant;

import java.util.List;

public interface Dispatcher {

     Routes assembleRoutes(List<GenomicVariant> variants) throws DispatchException;

}
