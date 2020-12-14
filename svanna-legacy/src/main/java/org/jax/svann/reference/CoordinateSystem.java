package org.jax.svann.reference;

/**
 * Coordinate system is used when defining begin and end coordinates of an interval.
 */
public enum CoordinateSystem {

    /**
     * Coordinate system where the position is excluded when used to denote begin coordinate and included when used to
     * describe the end coordinate of an interval.
     */
    ZERO_BASED,

    /**
     * Coordinate system where the position is included when used to denote both start and stop coordinate of an interval.
     */
    ONE_BASED
}
