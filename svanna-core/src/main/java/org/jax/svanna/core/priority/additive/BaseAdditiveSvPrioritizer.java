package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.priority.SvPrioritizer;
import org.jax.svanna.core.priority.SvPriority;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

abstract class BaseAdditiveSvPrioritizer<D extends RouteData, R extends RouteResult> implements SvPrioritizer<SvPriority> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAdditiveSvPrioritizer.class);

    private final Dispatcher dispatcher;

    private final RouteDataService<D> routeDataService;

    private final RouteDataEvaluator<D, R> routeDataEvaluator;

    protected BaseAdditiveSvPrioritizer(Builder<?, D, R> builder) {
        this.dispatcher = Objects.requireNonNull(builder.dispatcher);
        this.routeDataService = Objects.requireNonNull(builder.routeDataService);
        this.routeDataEvaluator = Objects.requireNonNull(builder.routeDataEvaluator);
    }

    @Override
    public SvPriority prioritize(Variant variant) {
        try {
            Routes routes = dispatcher.assembleRoutes(List.of(variant));
            D data = routeDataService.getData(routes);
            R result = routeDataEvaluator.evaluate(data);
            return processRouteResult(result);
        } catch (IntrachromosomalBreakendException e) {
            LogUtils.logTrace(LOGGER, "Unable to create the annotation route for variant `{}`: {}", LogUtils.variantSummary(variant), e.getMessage());
            return SvPriority.unknown();
        } catch (DispatchException e) {
            LogUtils.logWarn(LOGGER, "Unable to create the annotation route for variant `{}`: {}", LogUtils.variantSummary(variant), e.getMessage());
            return SvPriority.unknown();
        } catch (Exception e) {
            // TODO - remove once stable
            LogUtils.logError(LOGGER, "Error: ", e);
            throw e;
        }
    }

    protected abstract SvPriority processRouteResult(R routeResult);


    public abstract static class Builder<T extends Builder<T, D, R>, D extends RouteData, R extends RouteResult> {

        private Dispatcher dispatcher;

        private RouteDataService<D> routeDataService;

        private RouteDataEvaluator<D, R> routeDataEvaluator;

        protected Builder() {}

        public T dispatcher(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
            return self();
        }

        public T routeDataService(RouteDataService<D> routeDataService) {
            this.routeDataService = routeDataService;
            return self();
        }

        public T routeDataEvaluator(RouteDataEvaluator<D, R> routeDataEvaluator) {
            this.routeDataEvaluator = routeDataEvaluator;
            return self();
        }

        protected  abstract BaseAdditiveSvPrioritizer<D, R> build();

        protected abstract T self();

    }
}
