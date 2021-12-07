package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;
import java.util.Objects;

public class Routes {

    private final List<GenomicRegion> references;
    private final List<Route> alternates;

    public static Routes of(List<GenomicRegion> references, List<Route> alternates) {
        return new Routes(references, alternates);
    }

    private Routes(List<GenomicRegion> references, List<Route> alternates) {
        this.references = references;
        this.alternates = alternates;
    }

    public List<GenomicRegion> references() {
        return references;
    }

    public List<Route> alternates() {
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
