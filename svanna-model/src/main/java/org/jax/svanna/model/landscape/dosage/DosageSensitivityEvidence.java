package org.jax.svanna.model.landscape.dosage;

import java.util.Objects;

public enum DosageSensitivityEvidence {

    NO_EVIDENCE,
    LITTLE_EVIDENCE,
    SOME_EVIDENCE,
    SUFFICIENT_EVIDENCE;

    public boolean isAtLeast(DosageSensitivityEvidence evidence) {
        Objects.requireNonNull(evidence, "Evidence must not be null");
        switch (evidence) {
            case NO_EVIDENCE:
                return true;
            case LITTLE_EVIDENCE:
                return this != NO_EVIDENCE;
            case SOME_EVIDENCE:
                return this == SOME_EVIDENCE || this == SUFFICIENT_EVIDENCE;
            case SUFFICIENT_EVIDENCE:
                return this == SUFFICIENT_EVIDENCE;
            default:
                throw new IllegalArgumentException("Unexpected value `" + evidence + "`");
        }
    }
}
