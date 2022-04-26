package org.monarchinitiative.svanna.core.priority.additive.impact;

import org.monarchinitiative.svanna.core.priority.additive.Event;
import org.monarchinitiative.svanna.core.priority.additive.Projection;
import org.monarchinitiative.sgenes.model.Located;

import java.util.Set;

public class SimpleSequenceImpactCalculator<T extends Located> implements SequenceImpactCalculator<T> {

    private final double factor;

    public SimpleSequenceImpactCalculator(double factor) {
        this.factor = factor;
    }

    @Override
    public double projectImpact(Projection<T> projection) {
        Set<Event> spannedEvents = projection.spannedEvents();
        Event startEvent = projection.startEvent();
        Event endEvent = projection.endEvent();

        if (spannedEvents.contains(Event.DELETION))
            // A part of the gene is deleted
            return 0.;

        if (spannedEvents.contains(Event.INSERTION))
            // Insertion localized somewhere within the element. This might be too stringent, wrongly penalizing
            // deep intronic insertions which might be benign
            return 0.;

        else if (spannedEvents.contains(Event.DUPLICATION))
            // Duplication localized somewhere within the gene, Again, might be too stringent and wrongly penalize small
            // deep intronic duplications
            return 0.;

        else if (spannedEvents.contains(Event.INVERSION))
            return 0.;

        else if (startEvent == Event.BREAKEND || endEvent == Event.BREAKEND)
            return 0.;

        // nothing wrong with the element
        return factor;
    }

    @Override
    public double noImpact() {
        return factor;
    }
}
