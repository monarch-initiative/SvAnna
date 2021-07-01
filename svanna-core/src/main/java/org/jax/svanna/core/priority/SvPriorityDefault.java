package org.jax.svanna.core.priority;

class SvPriorityDefault implements SvPriority {

    private final double priority;

    static SvPriorityDefault of(double priority) {
        return new SvPriorityDefault(priority);
    }

    private SvPriorityDefault(double priority) {
        this.priority = priority;
    }

    @Override
    public double getPriority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SvPriorityDefault that = (SvPriorityDefault) o;
        return Double.compare(that.priority, priority) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(priority);
    }

    @Override
    public String toString() {
        return "SvPriority{" +
                "priority=" + priority +
                '}';
    }
}
