package org.jax.svanna.core.priority.additive.impact;

import org.jax.svanna.core.LogUtils;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.priority.additive.Event;
import org.jax.svanna.core.priority.additive.Projection;
import org.jax.svanna.core.priority.additive.Segment;
import org.monarchinitiative.svart.Coordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EnhancerSequenceImpactCalculator implements SequenceImpactCalculator<Enhancer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnhancerSequenceImpactCalculator.class);

    private final Map<Event, Double> fitnessWithEvent;

    private final double enhancerFactor;

    public EnhancerSequenceImpactCalculator(double enhancerFactor) {
        this.enhancerFactor = enhancerFactor;
        this.fitnessWithEvent = Map.of(
                Event.GAP, enhancerFactor,
                Event.SNV, .85 * enhancerFactor,
                Event.DUPLICATION, .2 * enhancerFactor,
                Event.INSERTION, .2 * enhancerFactor,
                Event.DELETION, .1 * enhancerFactor,
                Event.INVERSION, .0,
                Event.BREAKEND, .0
        );
    }

    @Override
    public double projectImpact(Projection<Enhancer> projection) {
        return projection.isIntraSegment()
                ? processIntraSegmentProjection(projection)
                : processInterSegmentProjection(projection);
    }

    @Override
    public double noImpact() {
        return enhancerFactor;
    }

    private double processIntraSegmentProjection(Projection<Enhancer> projection) {
        switch (projection.startEvent()) {
            case DELETION:
                // the entire enhancer region is deleted
                return 0.;
            case DUPLICATION:
                // the entire enhancer is duplicated
                return 2. * noImpact();
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
                score = Math.min(fitnessWithEvent.getOrDefault(segment.event(), noImpact()), score);
        }

        return score;
    }
}
