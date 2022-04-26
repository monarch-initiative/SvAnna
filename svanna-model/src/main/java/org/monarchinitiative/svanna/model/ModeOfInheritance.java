package org.monarchinitiative.svanna.model;

public enum ModeOfInheritance {

    /**
     * autosomal dominant inheritance
     */
    AUTOSOMAL_DOMINANT,
    /**
     * autosomal recessive inheritance
     */
    AUTOSOMAL_RECESSIVE,
    /**
     * dominant inheritance on X chromosome
     */
    X_DOMINANT,
    /**
     * recessive inheritance on X chromosome
     */
    X_RECESSIVE,
    /**
     * Y-linked inheritance
     */
    Y_LINKED,
    /**
     * Mitochondrial inheritance
     */
    MITOCHONDRIAL,
    /**
     * value for encoding uninitialized/unknown values
     */
    UNKNOWN;

}
