package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.exception.SvAnnRuntimeException;

public class DispatchException extends SvAnnRuntimeException {

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
