package org.jax.svann.priority;

public enum SvImpact {
    UNKNOWN,
    LOW,
    INTERMEDIATE,
    HIGH;

    /**
     * Compare the current SvImpact with the threshold.
     * Note that if this is UNKNOWN, then we never satisfy a threshold
     *
     * @param threshold
     * @return true if this SvImpact is at least as high impact as threshold
     */
    public boolean satisfiesThreshold(SvImpact threshold) {
        if (this == HIGH)
            return true;
        if (this == INTERMEDIATE)
            return threshold != HIGH;
        if (this == LOW)
            return threshold == LOW;
        else
            return false;
    }



}
