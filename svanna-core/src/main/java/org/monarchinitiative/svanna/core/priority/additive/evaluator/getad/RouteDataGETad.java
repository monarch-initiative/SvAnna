package org.monarchinitiative.svanna.core.priority.additive.evaluator.getad;

import org.monarchinitiative.svanna.core.priority.additive.RouteData;
import org.monarchinitiative.svanna.core.priority.additive.Routes;
import org.monarchinitiative.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.svanna.model.landscape.tad.TadBoundary;
import org.monarchinitiative.sgenes.model.Gene;

import java.util.*;

/**
 * Data holder for variant evaluation.
 */
public class RouteDataGETad extends RouteData {

    private final Set<Gene> genes;
    private final Set<Enhancer> enhancers;
    private final List<TadBoundary> tadBoundaries;


    protected RouteDataGETad(Builder builder) {
        super(builder.routes);
        this.genes = builder.genes;
        this.enhancers = builder.enhancers;
        this.tadBoundaries = builder.boundaries;
    }

    public Set<Gene> genes() {
        return genes;
    }

    public Set<Enhancer> enhancers() {
        return enhancers;
    }

    public List<TadBoundary> tadBoundaries() {
        return tadBoundaries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RouteDataGETad that = (RouteDataGETad) o;
        return Objects.equals(genes, that.genes) && Objects.equals(enhancers, that.enhancers) && Objects.equals(tadBoundaries, that.tadBoundaries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), genes, enhancers, tadBoundaries);
    }

    @Override
    public String toString() {
        return "RouteDataGE{" +
                "genes=" + genes +
                ", enhancers=" + enhancers +
                ", tadBoundaries=" + tadBoundaries +
                '}';
    }

    public static Builder builder(Routes route) {
        return new Builder(route);
    }

    public static class Builder {

        private final Routes routes;

        private final Set<Gene> genes;
        private final Set<Enhancer> enhancers;
        private final List<TadBoundary> boundaries;

        private Builder(Routes routes) {
            this.routes = Objects.requireNonNull(routes);
            this.genes = new HashSet<>();
            this.enhancers = new HashSet<>();
            this.boundaries = new LinkedList<>();
        }

        public Builder addGene(Gene gene) {
            this.genes.add(gene);
            return this;
        }

        public Builder addGenes(Collection<Gene> genes) {
            this.genes.addAll(genes);
            return this;
        }

        public Builder addEnhancers(Collection<Enhancer> enhancers) {
            this.enhancers.addAll(enhancers);
            return this;
        }

        public Builder addEnhancer(Enhancer enhancer) {
            this.enhancers.add(enhancer);
            return this;
        }

        public Builder addTadBoundary(TadBoundary boundary) {
            this.boundaries.add(boundary);
            return this;
        }

        public Builder addTadBoundaries(List<TadBoundary> boundaries) {
            this.boundaries.addAll(boundaries);
            return this;
        }

        public RouteDataGETad build() {
            return new RouteDataGETad(this);
        }
    }
}
