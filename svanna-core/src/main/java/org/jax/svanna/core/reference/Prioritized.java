package org.jax.svanna.core.reference;

import org.jax.svanna.core.prioritizer.SvPriority;

// TODO - remove generics after DiscreteSvPriority is removed
public interface Prioritized<T extends SvPriority> {

    void setSvPriority(T priority);

    T svPriority();
}
