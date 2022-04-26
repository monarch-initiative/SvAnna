package org.monarchinitiative.svanna.core.priority.additive;

public enum Event {

    UNKNOWN,
    GAP,
    // TODO - it may make more sense to replace deletion/duplication with a CNV and n copies, where normal == 1.
    DELETION,
    DUPLICATION,
    INSERTION,
    INVERSION,
    SNV,
    BREAKEND

}
