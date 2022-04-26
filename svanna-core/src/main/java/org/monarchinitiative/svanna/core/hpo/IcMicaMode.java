package org.monarchinitiative.svanna.core.hpo;

/**
 * Describe the way how to get information content (IC) of the most informative common ancestor (MICA) for two
 * HPO terms.
 */
public enum IcMicaMode {
    /**
     * All positive <code>IC(MICA(t1,t2))</code> values are loaded into main memory. This mode is faster but more
     * memory intensive.
     */
    IN_MEMORY,

    /**
     * The <code>IC(MICA(t1,t2))</code> values are retrieved using individual database lookups, when required.
     */
    DATABASE
}
