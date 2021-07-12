package org.jax.svanna.core.priority;

/**
 * Interface that represents result of gene prioritization, e.g. the <em>TAD<sub>SV</sub></em> score
 */
public interface SvPriority extends Comparable<SvPriority> {

    static SvPriority unknown() {
        return UnknownSvPriority.instance();
    }

    static SvPriority of(double priority) {
        return SvPriorityDefault.of(priority);
    }

    /**
     * @return priority as double, higher value means higher priority
     */
    double getPriority();

    @Override
    default int compareTo(SvPriority o) {
        return Double.compare(getPriority(), o.getPriority());
    }

}
