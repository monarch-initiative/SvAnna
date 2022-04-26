package org.monarchinitiative.svanna.configuration.exception;

/**
 * An exception thrown when a resource file (e.g. a database file) is missing from SvAnna data directory.
 *
 * @author Daniel Danis
 */
public class MissingResourceException extends Exception {


    public MissingResourceException() {
        super();
    }

    public MissingResourceException(String message) {
        super(message);
    }

    public MissingResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingResourceException(Throwable cause) {
        super(cause);
    }

    protected MissingResourceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
