package org.jax.svanna.core.priority.additive.ge;

import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.priority.additive.RouteData;
import org.jax.svanna.core.priority.additive.Routes;
import org.jax.svanna.core.reference.Gene;

import java.util.*;

/**
 * Data holder for variant evaluation.
 */
public class RouteDataGE extends RouteData {

    private final Set<Gene> refGenes;
    private final Set<Gene> altGenes;
    private final Set<Enhancer> refEnhancers;
    private final Set<Enhancer> altEnhancers;
    private final List<TadBoundary> refTadBoundaries;
    private final List<TadBoundary> altTadBoundaries;

    protected RouteDataGE(Builder builder) {
        super(builder.route);
        this.refGenes = builder.refGenes;
        this.altGenes = builder.altGenes;
        this.refEnhancers = builder.refEnhancers;
        this.altEnhancers = builder.altEnhancers;
        this.refTadBoundaries = builder.refBoundaries;
        this.altTadBoundaries = builder.altBoundaries;
    }

    public Set<Gene> refGenes() {
        return refGenes;
    }

    public Set<Gene> altGenes() {
        return altGenes;
    }

    public Set<Enhancer> refEnhancers() {
        return refEnhancers;
    }

    public Set<Enhancer> altEnhancers() {
        return altEnhancers;
    }

    public List<TadBoundary> refTadBoundaries() {
        return refTadBoundaries;
    }

    public List<TadBoundary> altTadBoundaries() {
        return altTadBoundaries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RouteDataGE that = (RouteDataGE) o;
        return Objects.equals(refGenes, that.refGenes) && Objects.equals(altGenes, that.altGenes) && Objects.equals(refEnhancers, that.refEnhancers) && Objects.equals(altEnhancers, that.altEnhancers) && Objects.equals(refTadBoundaries, that.refTadBoundaries) && Objects.equals(altTadBoundaries, that.altTadBoundaries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), refGenes, altGenes, refEnhancers, altEnhancers, refTadBoundaries, altTadBoundaries);
    }

    @Override
    public String toString() {
        return "RouteDataGE{" +
                "refGenes=" + refGenes +
                ", altGenes=" + altGenes +
                ", refEnhancers=" + refEnhancers +
                ", altEnhancers=" + altEnhancers +
                ", refTadBoundaries=" + refTadBoundaries +
                ", altTadBoundaries=" + altTadBoundaries +
                '}';
    }

    public static Builder builder(Routes route) {
        return new Builder(route);
    }

    public static class Builder {

        private final Routes route;

        private final Set<Gene> refGenes;
        private final Set<Gene> altGenes;

        private final Set<Enhancer> refEnhancers;
        private final Set<Enhancer> altEnhancers;

        private final List<TadBoundary> refBoundaries;
        private final List<TadBoundary> altBoundaries;

        private Builder(Routes route) {
            this.route = Objects.requireNonNull(route);
            this.refGenes = new HashSet<>();
            this.altGenes = new HashSet<>();
            this.refEnhancers = new HashSet<>();
            this.altEnhancers = new HashSet<>();
            this.refBoundaries = new ArrayList<>();
            this.altBoundaries = new ArrayList<>();
        }

        public Builder addRefGene(Gene genes) {
            this.refGenes.add(genes);
            return this;
        }

        public Builder addRefGenes(Collection<Gene> genes) {
            this.refGenes.addAll(genes);
            return this;
        }

        public Builder addAltGene(Gene genes) {
            this.altGenes.add(genes);
            return this;
        }

        public Builder addAltGenes(Collection<Gene> genes) {
            this.altGenes.addAll(genes);
            return this;
        }

        public Builder addRefEnhancers(Collection<Enhancer> enhancers) {
            this.refEnhancers.addAll(enhancers);
            return this;
        }

        public Builder addRefEnhancer(Enhancer enhancers) {
            this.refEnhancers.add(enhancers);
            return this;
        }

        public Builder addAltEnhancers(Collection<Enhancer> enhancers) {
            this.altEnhancers.addAll(enhancers);
            return this;
        }

        public Builder addAltEnhancer(Enhancer enhancers) {
            this.altEnhancers.add(enhancers);
            return this;
        }

        public Builder addAltTadBoundaries(List<TadBoundary> boundaries) {
            this.altBoundaries.addAll(boundaries);
            return this;
        }

        public Builder addRefTadBoundary(TadBoundary boundary) {
            this.refBoundaries.add(boundary);
            return this;
        }

        public Builder addAltTadBoundary(TadBoundary boundary) {
            this.altBoundaries.add(boundary);
            return this;
        }

        public Builder addRefTadBoundaries(List<TadBoundary> boundaries) {
            this.refBoundaries.addAll(boundaries);
            return this;
        }

        public RouteDataGE build() {
            return new RouteDataGE(this);
        }
    }
}
