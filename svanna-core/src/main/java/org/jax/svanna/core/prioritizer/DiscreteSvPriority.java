package org.jax.svanna.core.prioritizer;

public interface DiscreteSvPriority extends SvPriority {

    static DiscreteSvPriority of(SvImpact impact, boolean hasPhenotypicRelevance) {
        return DiscreteSvPriorityDefault.of(impact, hasPhenotypicRelevance);
    }

    static DiscreteSvPriority unknown() {
        return DiscreteSvPriorityDefault.unknown();
    }

    @Override
    default double getPriority() {
        return getImpact().priority();
    }

    SvImpact getImpact();

}
