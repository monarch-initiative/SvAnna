package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class VariantEvaluator<V extends Variant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariantEvaluator.class);

    private final Dispatcher<V> dispatcher;

//    private final LandscapeSource<L> landscapeSource;

//    private final LandscapeEvaluator<L> landscapeEvaluator;

    protected VariantEvaluator(Builder<V> builder) {
        this.dispatcher = Objects.requireNonNull(builder.dispatcher);
//        this.landscapeSource = Objects.requireNonNull(builder.landscapeSource);
//        this.landscapeEvaluator = Objects.requireNonNull(builder.landscapeEvaluator);
    }

    public double evaluate(List<V> variants) {
        // Create routes for ref and alt allele.
        Routes routes;
        try {
            routes = dispatcher.assembleRoutes(variants);
        } catch (DispatchException e) {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Unable to create the annotation route for variants `{}`: {}", variants, e.getMessage());
            return Double.NaN;
        }



        // Create landscapes for routes - trip to the database to fetch enhancers, genes/transcripts, etc.
//        Landscapes<L> landscapes = landscapeSource.annotatePath(routes);
        // Evaluate impact of variant on the landscapes one by one and sum the result
//        double landscapeScore = landscapes.alternate().stream()
//                .mapToDouble(l -> landscapeEvaluator.evaluateLandscape(variants, l))
//                .sum();
        double landscapeScore = 0.;

        return Math.abs(landscapeScore);
    }

    public static class Builder<V extends Variant> {

        private Dispatcher<V> dispatcher;

//        private LandscapeSource<L> landscapeSource;
//
//        private LandscapeEvaluator<L> landscapeEvaluator;

        public Builder<V> dispatcher(Dispatcher<V> dispatcher) {
            this.dispatcher = dispatcher;
            return self();
        }

//        public Builder<V, L> landscapeSource(LandscapeSource<L> landscapeSource) {
//            this.landscapeSource = landscapeSource;
//            return self();
//        }
//
//        public Builder<V, L> landscapeEvaluator(LandscapeEvaluator<L> landscapeEvaluator) {
//            this.landscapeEvaluator = landscapeEvaluator;
//            return self();
//        }

        public VariantEvaluator<V> build() {
            return new VariantEvaluator<>(self());
        }

        protected Builder<V> self() {
            return this;
        }

    }

}
