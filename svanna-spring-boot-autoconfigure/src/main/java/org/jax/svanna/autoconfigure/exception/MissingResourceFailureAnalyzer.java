package org.jax.svanna.autoconfigure.exception;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * @author Daniel Danis
 */
public class MissingResourceFailureAnalyzer extends AbstractFailureAnalyzer<MissingResourceException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MissingResourceException cause) {
        return new FailureAnalysis(String.format("SvAnna could not be auto-configured properly: '%s'", cause.getMessage()),
                "This issue would likely be solved by re-downloading and re-creating SvAnna data directory",
                cause);
    }
}
