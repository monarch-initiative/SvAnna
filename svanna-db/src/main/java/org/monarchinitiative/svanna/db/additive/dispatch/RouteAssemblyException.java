package org.monarchinitiative.svanna.db.additive.dispatch;

import org.monarchinitiative.svanna.core.priority.additive.DispatchException;

public class RouteAssemblyException extends DispatchException {

    public RouteAssemblyException() {
        super();
    }

    public RouteAssemblyException(String message) {
        super(message);
    }

    public RouteAssemblyException(String message, Throwable cause) {
        super(message, cause);
    }

    public RouteAssemblyException(Throwable cause) {
        super(cause);
    }

    protected RouteAssemblyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
