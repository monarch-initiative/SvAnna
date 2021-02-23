package org.jax.svanna.core.priority.additive.landscape;

import org.jax.svanna.core.reference.Gene;

import java.util.List;

/**
 * Roughly corresponds to a TAD region.
 */
@Deprecated(forRemoval = true)
public abstract class Landscape {

    private final String id;

    private final List<Gene> genes;

    protected Landscape(String id, List<Gene> genes) {
        this.id = id;
        this.genes = genes;
    }

    public String id() {
        return id;
    }

    public List<Gene> genes() {
        return genes;
    }

}
