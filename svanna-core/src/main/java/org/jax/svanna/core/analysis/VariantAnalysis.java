package org.jax.svanna.core.analysis;

import org.jax.svanna.core.reference.SvannaVariant;

import java.util.stream.Stream;

public interface VariantAnalysis<T extends SvannaVariant> {

     Stream<T> analyze(Stream<T> variants);

}
