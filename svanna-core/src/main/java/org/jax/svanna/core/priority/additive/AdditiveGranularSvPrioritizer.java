package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.priority.SvPriority;
import org.jax.svanna.core.priority.additive.ge.RouteDataGE;

public class AdditiveGranularSvPrioritizer extends BaseAdditiveSvPrioritizer<RouteDataGE, GranularRouteResult> {


    protected AdditiveGranularSvPrioritizer(Builder builder) {
        super(builder);
    }

    @Override
    protected SvPriority processRouteResult(GranularRouteResult routeResult) {
        return routeResult;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseAdditiveSvPrioritizer.Builder<Builder, RouteDataGE, GranularRouteResult> {

        private Builder() {}

        @Override
        public BaseAdditiveSvPrioritizer<RouteDataGE, GranularRouteResult> build() {
            return new AdditiveGranularSvPrioritizer(self());
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
