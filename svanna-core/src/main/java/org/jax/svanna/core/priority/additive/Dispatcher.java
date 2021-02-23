package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.Variant;

import java.util.List;

public interface Dispatcher<T extends Variant> {

    // create routes for the variant. Figure out upstream and downstream TAD boundaries based on alt allele, identify
    // deletion of TAD boundaries.
    //
    // If the variant deletes one or more TADs, then the ref route might consist of 2+ segments, while the alt route
    // contains a single segment only.
    Routes assembleRoutes(List<T> variants) throws DispatchException;

}
