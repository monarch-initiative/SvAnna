package org.jax.svanna.model.landscape;

import org.monarchinitiative.svart.GenomicRegion;
import xyz.ielis.silent.genes.model.Located;

import java.util.Objects;

public abstract class BaseLocated implements Located {

    private final GenomicRegion location;

    protected BaseLocated(GenomicRegion location) {
        this.location = Objects.requireNonNull(location, "Location must not be null");
    }

    @Override
    public GenomicRegion location() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseLocated that = (BaseLocated) o;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    @Override
    public String toString() {
        return "BaseLocated{" +
                "location=" + location +
                '}';
    }
}
