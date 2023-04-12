package org.monarchinitiative.svanna.cli.cmd;

import org.monarchinitiative.svanna.core.SvAnnaRuntimeException;

/**
 * An exception thrown if inputs for the analysis are incomplete or otherwise invalid.
 */
class AnalysisInputException extends SvAnnaRuntimeException {
    AnalysisInputException() {
        super();
    }

    AnalysisInputException(String message) {
        super(message);
    }

    AnalysisInputException(String message, Throwable cause) {
        super(message, cause);
    }

    AnalysisInputException(Throwable cause) {
        super(cause);
    }

    AnalysisInputException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
