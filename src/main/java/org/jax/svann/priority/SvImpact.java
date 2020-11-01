package org.jax.svann.priority;

public enum SvImpact {
    HIGH_IMPACT,
    INTERMEDIATE_IMPACT,
    LOW_IMPACT,
    UNKNOWN;

    /**
     * Compare the current SvImpact with the threshold.
     * Note that if this is UNKNOWN, then we never satisfy a threshold
     * @param threshold
     * @return true if this SvImpact is at least as high impact as threshold
     */
    public boolean satisfiesThreshold(SvImpact threshold) {
        if (this == HIGH_IMPACT)
            return true;
        if (this == INTERMEDIATE_IMPACT)
            return threshold != HIGH_IMPACT;
        if (this == LOW_IMPACT)
            return threshold == LOW_IMPACT;
        else
            return false;
    }



}
