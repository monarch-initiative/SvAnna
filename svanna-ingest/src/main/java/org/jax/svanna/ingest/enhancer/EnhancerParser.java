package org.jax.svanna.ingest.enhancer;

import org.jax.svanna.core.reference.Enhancer;

import java.util.List;

public interface EnhancerParser<T extends Enhancer> {

    List<? extends T> parse();
}
