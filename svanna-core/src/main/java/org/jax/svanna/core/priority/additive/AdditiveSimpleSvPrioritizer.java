package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.priority.SvPriority;


public class AdditiveSimpleSvPrioritizer<D extends RouteData> extends BaseAdditiveSvPrioritizer<D, RouteResult> {

    protected AdditiveSimpleSvPrioritizer(Builder<D> builder) {
        super(builder);
    }

    @Override
    protected SvPriority processRouteResult(RouteResult routeResult) {
        return SvPriority.of(routeResult.priority());
    }

    public static <D extends RouteData> Builder<D> builder() {
        return new Builder<>();
    }

    public static class Builder<D extends RouteData> extends BaseAdditiveSvPrioritizer.Builder<Builder<D>, D, RouteResult> {

        protected Builder() {}

        public AdditiveSimpleSvPrioritizer<D> build() {
            return new AdditiveSimpleSvPrioritizer<>(self());
        }

        protected Builder<D> self() {
            return this;
        }

    }

}
