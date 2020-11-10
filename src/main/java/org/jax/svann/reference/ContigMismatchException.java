package org.jax.svann.reference;

import org.jax.svann.except.SvAnnRuntimeException;

public class ContigMismatchException extends SvAnnRuntimeException {
    public ContigMismatchException() {
        super();
    }

    public ContigMismatchException(String message) {
        super(message);
    }

    public ContigMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public ContigMismatchException(Throwable cause) {
        super(cause);
    }

    protected ContigMismatchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
