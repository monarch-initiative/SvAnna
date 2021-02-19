package org.jax.svanna.core.prioritizer;

import java.util.Objects;

class DiscreteSvPriorityDefault implements DiscreteSvPriority {

    private static final DiscreteSvPriorityDefault UNKNOWN = new DiscreteSvPriorityDefault(SvImpact.UNKNOWN, false);

    private final SvImpact impact;
    private final boolean hasPhenotypicRelevance;

    private DiscreteSvPriorityDefault(SvImpact impact, boolean hasPhenotypicRelevance) {
        this.impact = impact;
        this.hasPhenotypicRelevance = hasPhenotypicRelevance;
    }

    static DiscreteSvPriorityDefault unknown() {
        return UNKNOWN;
    }


    static DiscreteSvPriorityDefault of(SvImpact impact, boolean hasPhenotypicRelevance) {
        return new DiscreteSvPriorityDefault(impact, hasPhenotypicRelevance);
    }

    @Override
    public SvImpact getImpact() {
        return impact;
    }

    @Override
    public boolean hasPhenotypicRelevance() {
        return hasPhenotypicRelevance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscreteSvPriorityDefault that = (DiscreteSvPriorityDefault) o;
        return hasPhenotypicRelevance == that.hasPhenotypicRelevance && impact == that.impact;
    }

    @Override
    public int hashCode() {
        return Objects.hash(impact, hasPhenotypicRelevance);
    }

    @Override
    public String toString() {
        return "DiscreteSvPriorityDefault{" +
                "impact=" + impact +
                ", hasPhenotypicRelevance=" + hasPhenotypicRelevance +
                '}';
    }
}
