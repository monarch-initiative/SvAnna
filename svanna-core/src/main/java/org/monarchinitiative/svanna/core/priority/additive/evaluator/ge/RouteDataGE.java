package org.monarchinitiative.svanna.core.priority.additive.evaluator.ge;

import org.monarchinitiative.svanna.core.priority.additive.RouteData;
import org.monarchinitiative.svanna.core.priority.additive.Routes;
import org.monarchinitiative.svanna.model.landscape.enhancer.Enhancer;
import org.monarchinitiative.sgenes.model.Gene;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Data holder for variant evaluation.
 */
public class RouteDataGE extends RouteData {

    private final Set<Gene> genes;
    private final Set<Enhancer> enhancers;

    protected RouteDataGE(Builder builder) {
        super(builder.routes);
        this.genes = builder.genes;
        this.enhancers = builder.enhancers;
    }

    public static Builder builder(Routes route) {
        return new Builder(route);
    }

    public Set<Gene> genes() {
        return genes;
    }

    public Set<Enhancer> enhancers() {
        return enhancers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RouteDataGE that = (RouteDataGE) o;
        return Objects.equals(genes, that.genes) && Objects.equals(enhancers, that.enhancers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), genes, enhancers);
    }

    @Override
    public String toString() {
        return "RouteDataGE{" +
                "genes=" + genes +
                ", enhancers=" + enhancers +
                "} " + super.toString();
    }

    public static class Builder {

        private final Routes routes;

        private final Set<Gene> genes;
        private final Set<Enhancer> enhancers;

        private Builder(Routes routes) {
            this.routes = Objects.requireNonNull(routes);
            this.genes = new HashSet<>();
            this.enhancers = new HashSet<>();
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

        public RouteDataGE build() {
            return new RouteDataGE(this);
        }
    }
}
