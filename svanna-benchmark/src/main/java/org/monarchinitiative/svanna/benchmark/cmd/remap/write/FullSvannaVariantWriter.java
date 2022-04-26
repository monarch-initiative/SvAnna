package org.monarchinitiative.svanna.benchmark.cmd.remap.write;

import org.monarchinitiative.svanna.io.FullSvannaVariant;

public interface FullSvannaVariantWriter {

    int write(Iterable<FullSvannaVariant> variants);

}
