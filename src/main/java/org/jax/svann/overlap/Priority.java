package org.jax.svann.overlap;

import org.jax.svann.except.SvAnnRuntimeException;

public enum Priority {

    UNKNOWN,LOW,MODIFIER,HIGH;

    static Priority fromString(String s) {
        switch (s.toUpperCase()) {
            case "LOW":
                return LOW;
            case "MODIFIER":
                return MODIFIER;
            case "HIGH":
                return HIGH;
            default:
                throw new SvAnnRuntimeException("Did not recognize case" + s);
        }
    }
}
