package org.jax.svanna.autoconfigure.exception;

/**
 * This exception is thrown during auto-configuration, if an information that should have been provided by the user
 * is missing or if the information is not well-formatted.
 *
 * @author Daniel Danis
 */
public class UndefinedResourceException extends Exception {

    public UndefinedResourceException() {
        super();
    }

    public UndefinedResourceException(String message) {
        super(message);
    }

    public UndefinedResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public UndefinedResourceException(Throwable cause) {
        super(cause);
    }

    protected UndefinedResourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
