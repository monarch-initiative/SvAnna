package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.exception.SvAnnRuntimeException;

public class EvaluationException extends SvAnnRuntimeException {

    public EvaluationException() {
        super();
    }

    public EvaluationException(String message) {
        super(message);
    }

    public EvaluationException(String message, Throwable cause) {
        super(message, cause);
    }

    public EvaluationException(Throwable cause) {
        super(cause);
    }

    protected EvaluationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
