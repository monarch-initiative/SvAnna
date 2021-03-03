package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.landscape.Enhancer;
import org.monarchinitiative.svart.Coordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EnhancerSequenceImpactCalculator implements SequenceImpactCalculator<Enhancer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancerSequenceImpactCalculator.class);

    private static final Map<Event, Double> FITNESS_WITH_EVENT = Map.of(
            Event.GAP, 1.,
            Event.SNV, .85,
            Event.DUPLICATION, .2,
            Event.INSERTION, .2,
            Event.DELETION, .1,
            Event.INVERSION, .0,
            Event.BREAKEND, .0
    );

    @Override
    public double projectImpact(Projection<Enhancer> projection) {
        return projection.isIntraSegment()
                ? processIntraSegmentProjection(projection)
                : processInterSegmentProjection(projection);
    }

    private double processIntraSegmentProjection(Projection<Enhancer> projection) {
        switch (projection.startEvent()) {
            case DELETION:
                // the entire enhancer region is deleted
                return 0.;
            case DUPLICATION:
                // the entire gene is duplicated
                return 2.;
            case BREAKEND:
            case SNV:
            case INSERTION:
                // should not happen since these are event
                LogUtils.logWarn(LOGGER, "Enhancer is unexpectedly located within a {}. " +
                        "This should not have happened since length of {} on reference contig should be too small to encompass an enhancer", projection.startEvent(), projection.startEvent());
                return noImpact();
            default:
                LogUtils.logWarn(LOGGER, "Unable to process unknown impact");
            case GAP: // no impact
            case INVERSION: // the entire enhancer is inverted, this should not be an issue
                return noImpact();
        }
    }

    private double processInterSegmentProjection(Projection<Enhancer> projection) {
        double score = noImpact();

        Enhancer enhancer = projection.source();
        for (Segment segment : projection.spannedSegments()) {
            if (Coordinates.overlap(segment.coordinateSystem(), segment.startOnStrand(enhancer.strand()), segment.endOnStrand(enhancer.strand()),
                    enhancer.coordinateSystem(), enhancer.start(), enhancer.end()))
                score = Math.min(FITNESS_WITH_EVENT.getOrDefault(segment.event(), noImpact()), score);
        }

        return score;
    }
}
