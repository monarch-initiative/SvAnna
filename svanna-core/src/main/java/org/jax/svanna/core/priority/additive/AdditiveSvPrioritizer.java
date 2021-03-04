package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPriority;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class AdditiveSvPrioritizer<V extends Variant, D extends RouteData> implements SvPrioritizer<V, SvPriority> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdditiveSvPrioritizer.class);

    private final Dispatcher dispatcher;

    private final RouteDataService<D> routeDataService;

    private final RouteDataEvaluator<D> routeDataEvaluator;

    protected AdditiveSvPrioritizer(Builder<V, D> builder) {
        this.dispatcher = Objects.requireNonNull(builder.dispatcher);
        this.routeDataService = Objects.requireNonNull(builder.routeDataService);
        this.routeDataEvaluator = Objects.requireNonNull(builder.routeDataEvaluator);
    }

    @Override
    public SvPriority prioritize(V variant) {
        try {
            Routes routes;
            try {
                routes = dispatcher.assembleRoutes(List.of(variant));
            } catch (DispatchException e) {
                LogUtils.logWarn(LOGGER, "Unable to create the annotation route for variant `{}`: {}", variant, e.getMessage());
                return SvPriority.unknown();
            }

            D data = routeDataService.getData(routes);
            double score = routeDataEvaluator.evaluate(data);

            return SvPriority.of(score, true);
        } catch (Exception e) {
            // TODO - remove once stable
            LogUtils.logError(LOGGER, "Error: ", e);
            throw e;
        }
    }

    public static <V extends Variant, D extends RouteData> Builder<V,D> builder() {
        return new Builder<>();
    }

    public static class Builder<V extends Variant, D extends RouteData> {

        private Dispatcher dispatcher;

        private RouteDataService<D> routeDataService;

        private RouteDataEvaluator<D> routeDataEvaluator;

        protected Builder() {

        }

        public Builder<V, D> dispatcher(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
            return self();
        }

        public Builder<V, D> routeDataService(RouteDataService<D> routeDataService) {
            this.routeDataService = routeDataService;
            return self();
        }

        public Builder<V, D> routeDataEvaluator(RouteDataEvaluator<D> routeDataEvaluator) {
            this.routeDataEvaluator = routeDataEvaluator;
            return self();
        }

        public AdditiveSvPrioritizer<V, D> build() {
            return new AdditiveSvPrioritizer<>(self());
        }

        protected Builder<V, D> self() {
            return this;
        }

    }

}
