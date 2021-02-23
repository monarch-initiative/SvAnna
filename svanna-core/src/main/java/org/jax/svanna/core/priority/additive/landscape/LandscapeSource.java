package org.jax.svanna.core.priority.additive.landscape;

import org.jax.svanna.core.priority.additive.Routes;

@Deprecated(forRemoval = true)
public interface LandscapeSource<T extends Landscape> {

    // trip to the database to create track with enhancers, transcripts,
    Landscapes<T> annotatePath(Routes path);

}
