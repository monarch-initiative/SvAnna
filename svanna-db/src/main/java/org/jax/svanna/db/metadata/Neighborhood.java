package org.jax.svanna.db.metadata;

import org.monarchinitiative.svart.GenomicRegion;

import java.util.Objects;

class Neighborhood {

    private final GenomicRegion upstream;
    private final GenomicRegion downstreamRef;
    private final GenomicRegion downstreamAlt;

    static Neighborhood of(GenomicRegion upstream, GenomicRegion downstreamRef, GenomicRegion downstreamAlt) {
        return new Neighborhood(upstream, downstreamRef, downstreamAlt);
    }

    private Neighborhood(GenomicRegion upstream, GenomicRegion downstreamRef, GenomicRegion downstreamAlt) {
        this.upstream = upstream;
        this.downstreamRef = downstreamRef;
        this.downstreamAlt = downstreamAlt;
    }

    GenomicRegion upstream() {
        return upstream;
    }

    GenomicRegion downstreamRef() {
        return downstreamRef;
    }

    GenomicRegion downstreamAlt() {
        return downstreamAlt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Neighborhood that = (Neighborhood) o;
        return Objects.equals(upstream, that.upstream) && Objects.equals(downstreamRef, that.downstreamRef) && Objects.equals(downstreamAlt, that.downstreamAlt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(upstream, downstreamRef, downstreamAlt);
    }

    @Override
    public String toString() {
        return "Neighborhood{" +
                "upstream=" + upstream +
                ", downstreamRef=" + downstreamRef +
                ", downstreamAlt=" + downstreamAlt +
                '}';
    }
}
