package org.monarchinitiative.svanna.core.priority.additive;

import org.monarchinitiative.svanna.core.priority.SvPriority;

public class AdditiveGranularSvPrioritizer<DATA extends RouteData> extends BaseAdditiveSvPrioritizer<DATA, GranularRouteResult> {


    protected AdditiveGranularSvPrioritizer(Builder<DATA> builder) {
        super(builder);
    }

    @Override
    protected SvPriority processRouteResult(GranularRouteResult routeResult) {
        return routeResult;
    }

    public static <T extends RouteData> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<DATA extends RouteData> extends BaseAdditiveSvPrioritizer.Builder<Builder<DATA>, DATA, GranularRouteResult> {

        private Builder() {}

        @Override
        public BaseAdditiveSvPrioritizer<DATA, GranularRouteResult> build() {
            return new AdditiveGranularSvPrioritizer<>(self());
        }

        @Override
        protected Builder<DATA> self() {
            return this;
        }
    }
}
