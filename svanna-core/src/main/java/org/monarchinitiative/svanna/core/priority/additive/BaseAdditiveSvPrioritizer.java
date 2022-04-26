package org.monarchinitiative.svanna.core.priority.additive;

import org.monarchinitiative.svanna.core.LogUtils;
import org.monarchinitiative.svanna.core.priority.SvPrioritizer;
import org.monarchinitiative.svanna.core.priority.SvPriority;
import org.monarchinitiative.svart.GenomicVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

abstract class BaseAdditiveSvPrioritizer<DATA extends RouteData, RESULT extends RouteResult> implements SvPrioritizer<SvPriority> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseAdditiveSvPrioritizer.class);

    private final Dispatcher dispatcher;

    private final RouteDataService<DATA> routeDataService;

    private final RouteDataEvaluator<DATA, RESULT> routeDataEvaluator;

    protected BaseAdditiveSvPrioritizer(Builder<?, DATA, RESULT> builder) {
        this.dispatcher = Objects.requireNonNull(builder.dispatcher);
        this.routeDataService = Objects.requireNonNull(builder.routeDataService);
        this.routeDataEvaluator = Objects.requireNonNull(builder.routeDataEvaluator);
    }

    @Override
    public SvPriority prioritize(GenomicVariant variant) {
        try {
            Routes routes = dispatcher.assembleRoutes(List.of(variant));
            DATA data = routeDataService.getData(routes);
            RESULT result = routeDataEvaluator.evaluate(data);
            return processRouteResult(result);
        } catch (IntrachromosomalBreakendException e) {
            LOGGER.trace("Unable to create the annotation route for variant `{}`: {}", LogUtils.variantSummary(variant), e.getMessage());
            return SvPriority.unknown();
        } catch (DispatchException e) {
            LOGGER.debug("Unable to create the annotation route for variant `{}`: {}", LogUtils.variantSummary(variant), e.getMessage());
            return SvPriority.unknown();
        } catch (EvaluationException e) {
            LOGGER.warn("Error during evaluation of variant `{}`: {}", LogUtils.variantSummary(variant), e.getMessage());
            return SvPriority.unknown();
        }
    }

    protected abstract SvPriority processRouteResult(RESULT routeResult);


    protected abstract static class Builder<T extends Builder<T, DATA, RESULT>, DATA extends RouteData, RESULT extends RouteResult> {

        private Dispatcher dispatcher;

        private RouteDataService<DATA> routeDataService;

        private RouteDataEvaluator<DATA, RESULT> routeDataEvaluator;

        protected Builder() {}

        public T dispatcher(Dispatcher dispatcher) {
            this.dispatcher = dispatcher;
            return self();
        }

        public T routeDataService(RouteDataService<DATA> routeDataService) {
            this.routeDataService = routeDataService;
            return self();
        }

        public T routeDataEvaluator(RouteDataEvaluator<DATA, RESULT> routeDataEvaluator) {
            this.routeDataEvaluator = routeDataEvaluator;
            return self();
        }

        protected  abstract BaseAdditiveSvPrioritizer<DATA, RESULT> build();

        protected abstract T self();

    }
}
