package org.jax.svanna.core.priority;

import java.util.Objects;

class SvPriorityDefault implements SvPriority {

    private final double priority;
    private final boolean hasPhenotypicRelevance;

    static SvPriorityDefault of(double priority, boolean hasPhenotypicRelevance) {
        return new SvPriorityDefault(priority, hasPhenotypicRelevance);
    }

    private SvPriorityDefault(double priority, boolean hasPhenotypicRelevance) {
        this.priority = priority;
        this.hasPhenotypicRelevance = hasPhenotypicRelevance;
    }

    @Override
    public double getPriority() {
        return priority;
    }

    @Override
    public boolean hasPhenotypicRelevance() {
        return hasPhenotypicRelevance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SvPriorityDefault that = (SvPriorityDefault) o;
        return Double.compare(that.priority, priority) == 0 && hasPhenotypicRelevance == that.hasPhenotypicRelevance;
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, hasPhenotypicRelevance);
    }

    @Override
    public String toString() {
        return "SvPriorityDefault{" +
                "priority=" + priority +
                ", hasPhenotypicRelevance=" + hasPhenotypicRelevance +
                '}';
    }
}
