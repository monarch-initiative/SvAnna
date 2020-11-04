package org.jax.svann.reference;

/**
 * Package-private implementation of {@link CoordinatePair}.
 */
class SimpleCoordinatePair implements CoordinatePair {

    private final GenomicPosition start, end;

    private SimpleCoordinatePair(GenomicPosition start, GenomicPosition end) {
        this.start = start;
        this.end = end;
    }

    static SimpleCoordinatePair of(GenomicPosition start, GenomicPosition end) {
        return new SimpleCoordinatePair(start, end);
    }

    @Override
    public GenomicPosition getStart() {
        return start;
    }

    @Override
    public GenomicPosition getEnd() {
        return end;
    }
}
