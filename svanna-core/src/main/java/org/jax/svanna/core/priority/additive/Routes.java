package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.GenomicRegion;

public class Routes {

    private final GenomicRegion reference;
    private final Route alternate;

    public static Routes of(GenomicRegion reference, Route alternate) {
        return new Routes(reference, alternate);
    }

    private Routes(GenomicRegion reference, Route alternate) {
        this.reference = reference;
        this.alternate = alternate;
    }

    public GenomicRegion reference() {
        return reference;
    }

    public Route alternate() {
        return alternate;
    }
}
