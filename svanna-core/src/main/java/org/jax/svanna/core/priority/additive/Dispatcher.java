package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.Variant;

import java.util.List;

public interface Dispatcher {

     Routes assembleRoutes(List<Variant> variants) throws DispatchException;

}
