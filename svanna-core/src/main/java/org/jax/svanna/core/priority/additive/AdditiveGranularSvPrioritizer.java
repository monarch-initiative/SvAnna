package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.priority.SvPriority;
import org.monarchinitiative.svart.Variant;

public class AdditiveGranularSvPrioritizer<V extends Variant, D extends RouteData> extends BaseAdditiveSvPrioritizer<V, D, GranularRouteResult> {


    protected AdditiveGranularSvPrioritizer(Builder<V, D> builder) {
        super(builder);
    }

    @Override
    protected SvPriority processRouteResult(GranularRouteResult routeResult) {
        return routeResult;
    }

    public static <V extends Variant, D extends RouteData> Builder<V, D> builder() {
        return new Builder<>();
    }

    public static class Builder<V extends Variant, D extends RouteData> extends BaseAdditiveSvPrioritizer.Builder<Builder<V, D>, V, D, GranularRouteResult> {

        private Builder() {}

        @Override
        public AdditiveGranularSvPrioritizer<V, D> build() {
            return new AdditiveGranularSvPrioritizer<>(self());
        }

        @Override
        protected Builder<V, D> self() {
            return this;
        }

    }
}
