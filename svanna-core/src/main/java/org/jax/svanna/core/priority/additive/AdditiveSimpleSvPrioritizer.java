package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.priority.SvPriority;
import org.monarchinitiative.svart.Variant;


public class AdditiveSimpleSvPrioritizer<V extends Variant, D extends RouteData> extends BaseAdditiveSvPrioritizer<V, D, RouteResult> {

    protected AdditiveSimpleSvPrioritizer(Builder<V, D> builder) {
        super(builder);
    }

    @Override
    protected SvPriority processRouteResult(RouteResult routeResult) {
        return SvPriority.of(routeResult.priority());
    }

    public static <V extends Variant, D extends RouteData> Builder<V,D> builder() {
        return new Builder<>();
    }

    public static class Builder<V extends Variant, D extends RouteData> extends BaseAdditiveSvPrioritizer.Builder<Builder<V, D>, V, D, RouteResult> {

        protected Builder() {}

        public AdditiveSimpleSvPrioritizer<V, D> build() {
            return new AdditiveSimpleSvPrioritizer<>(self());
        }

        protected Builder<V, D> self() {
            return this;
        }

    }

}
