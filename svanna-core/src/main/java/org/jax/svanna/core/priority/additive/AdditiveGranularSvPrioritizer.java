package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.priority.SvPriority;

public class AdditiveGranularSvPrioritizer<D extends RouteData> extends BaseAdditiveSvPrioritizer<D, GranularRouteResult> {


    protected AdditiveGranularSvPrioritizer(Builder<D> builder) {
        super(builder);
    }

    @Override
    protected SvPriority processRouteResult(GranularRouteResult routeResult) {
        return routeResult;
    }

    public static <D extends RouteData> Builder<D> builder() {
        return new Builder<>();
    }

    public static class Builder<D extends RouteData> extends BaseAdditiveSvPrioritizer.Builder<Builder<D>, D, GranularRouteResult> {

        private Builder() {}

        @Override
        public AdditiveGranularSvPrioritizer<D> build() {
            return new AdditiveGranularSvPrioritizer<>(self());
        }

        @Override
        protected Builder<D> self() {
            return this;
        }

    }
}
