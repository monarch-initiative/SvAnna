package org.jax.svanna.autoconfigure.exception;

/**
 * This exception is thrown when a resource is corrupted or if the resource should be present but it is missing.
 * In contrast with {@link UndefinedResourceException}, this has nothing to do with the user.
 *
 * @author Daniel Danis
 */
public class InvalidResourceException extends Exception {

    public InvalidResourceException() {
        super();
    }

    public InvalidResourceException(String message) {
        super(message);
    }

    public InvalidResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidResourceException(Throwable cause) {
        super(cause);
    }

    protected InvalidResourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
