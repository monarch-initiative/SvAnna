package org.jax.svanna.core.priority.additive.landscape;

import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.reference.Gene;

import java.util.List;


@Deprecated(forRemoval = true)
public class LandscapeSimple extends Landscape {

    private final List<Enhancer> enhancers;

    private LandscapeSimple(String id, List<Gene> genes, List<Enhancer> enhancers) {
        super(id, genes);
        this.enhancers = enhancers;
    }

    public static LandscapeSimple of(String id, List<Gene> genes, List<Enhancer> enhancers) {
        // sort genes by position
        return new LandscapeSimple(id, genes, enhancers);
    }

    public List<Enhancer> enhancers() {
        return enhancers;
    }
}
