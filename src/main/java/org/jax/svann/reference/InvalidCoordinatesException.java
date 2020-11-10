package org.jax.svann.reference;

import org.jax.svann.except.SvAnnRuntimeException;

public class InvalidCoordinatesException extends SvAnnRuntimeException {

    public InvalidCoordinatesException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCoordinatesException(Throwable cause) {
        super(cause);
    }

    protected InvalidCoordinatesException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public InvalidCoordinatesException() {
        super();
    }

    public InvalidCoordinatesException(String m) {
        super(m);
    }
}
