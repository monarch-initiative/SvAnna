package org.jax.svanna.core.priority;

class UnknownSvPriority implements SvPriority {

    private static final UnknownSvPriority INSTANCE = new UnknownSvPriority();

    static UnknownSvPriority instance() {
        return INSTANCE;
    }

    private UnknownSvPriority() {}

    @Override
    public double getPriority() {
        return 0;
    }

    @Override
    public boolean hasPhenotypicRelevance() {
        return false;
    }

    @Override
    public String toString() {
        return "UNKNOWN PRIORITY";
    }
}
