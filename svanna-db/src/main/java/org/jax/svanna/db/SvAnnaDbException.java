package org.jax.svanna.db;

import org.jax.svanna.core.SvAnnaRuntimeException;

public class SvAnnaDbException extends SvAnnaRuntimeException {

    public SvAnnaDbException() {
        super();
    }

    public SvAnnaDbException(String message) {
        super(message);
    }

    public SvAnnaDbException(String message, Throwable cause) {
        super(message, cause);
    }

    public SvAnnaDbException(Throwable cause) {
        super(cause);
    }

    protected SvAnnaDbException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
