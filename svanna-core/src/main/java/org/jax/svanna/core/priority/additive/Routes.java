package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.GenomicRegion;

import java.util.Objects;
import java.util.Set;

public class Routes {

    private final Set<GenomicRegion> references;
    private final Set<Route> alternates;

    public static Routes of(Set<GenomicRegion> references, Set<Route> alternates) {
        return new Routes(references, alternates);
    }

    private Routes(Set<GenomicRegion> references, Set<Route> alternates) {
        this.references = references;
        this.alternates = alternates;
    }

    public Set<GenomicRegion> references() {
        return references;
    }

    public Set<Route> alternates() {
        return alternates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Routes that = (Routes) o;
        return Objects.equals(references, that.references) && Objects.equals(alternates, that.alternates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(references, alternates);
    }

    @Override
    public String toString() {
        return "Routes{" +
                "n references: " + references.size() +
                ", n alternates: " + alternates.size() +
                '}';
    }
}
