package org.jax.svanna.core.reference;

import org.jax.svanna.core.priority.SvPriority;

public interface Prioritized {

    void setSvPriority(SvPriority priority);

    SvPriority svPriority();
}
