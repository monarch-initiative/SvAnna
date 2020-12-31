package org.jax.svann.except;

public class SvAnnRuntimeException extends RuntimeException {
    public SvAnnRuntimeException() {
        super();
    }

    public SvAnnRuntimeException(String message) {
        super(message);
    }

    public SvAnnRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SvAnnRuntimeException(Throwable cause) {
        super(cause);
    }

    protected SvAnnRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
