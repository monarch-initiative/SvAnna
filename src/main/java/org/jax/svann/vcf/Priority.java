package org.jax.svann.vcf;

import org.jax.svann.except.L2ORuntimeException;

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
                throw new L2ORuntimeException("Did not recognize case" + s);
        }
    }
}
