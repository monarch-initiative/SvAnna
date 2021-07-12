package org.jax.svanna.core;

public class SvAnnaRuntimeException extends RuntimeException {
    public SvAnnaRuntimeException() {
        super();
    }

    public SvAnnaRuntimeException(String message) {
        super(message);
    }

    public SvAnnaRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SvAnnaRuntimeException(Throwable cause) {
        super(cause);
    }

    protected SvAnnaRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
