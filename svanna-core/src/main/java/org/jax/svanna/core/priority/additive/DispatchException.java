package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.SvAnnaRuntimeException;

public class DispatchException extends SvAnnaRuntimeException {

    public DispatchException() {
        super();
    }

    public DispatchException(String message) {
        super(message);
    }

    public DispatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public DispatchException(Throwable cause) {
        super(cause);
    }

    protected DispatchException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
