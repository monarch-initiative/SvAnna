package org.jax.svanna.benchmark.cmd.remap.write;

import org.jax.svanna.io.FullSvannaVariant;

public interface FullSvannaVariantWriter {

    int write(Iterable<FullSvannaVariant> variants);

}
