package org.jax.svanna.core.analysis;

import org.jax.svanna.core.reference.SvannaVariant;

import java.util.stream.Stream;

public interface VariantAnalysis {

     Stream<SvannaVariant> analyze(Stream<SvannaVariant> variants);

}
