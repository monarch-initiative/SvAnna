package org.jax.svanna.core.priority;

public interface SvPriority extends Comparable<SvPriority> {

    static SvPriority unknown() {
        return UnknownSvPriority.instance();
    }

    static SvPriority of(double priority, boolean hasPhenotypicRelevance) {
        return SvPriorityDefault.of(priority, hasPhenotypicRelevance);
    }

    /**
     * @return priority as double, higher value means higher priority
     */
    double getPriority();

    @Override
    default int compareTo(SvPriority o) {
        return Double.compare(getPriority(), o.getPriority());
    }

    /**
     * If true, the SV overlaps with a transcript or genomic regulatory element that is annotated
     * to an HPO term representing the phenotypic observations in the proband.
     *
     * @return true if the SV disrupts a gene or enhancer of potential phenotypic relevance.
     */
    boolean hasPhenotypicRelevance();
}
