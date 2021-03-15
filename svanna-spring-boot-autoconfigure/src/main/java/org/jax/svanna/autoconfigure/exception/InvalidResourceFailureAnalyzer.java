package org.jax.svanna.autoconfigure.exception;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * @author Daniel Danis
 */
public class InvalidResourceFailureAnalyzer extends AbstractFailureAnalyzer<InvalidResourceException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, InvalidResourceException cause) {
        return new FailureAnalysis(String.format("SvAnna could not be auto-configured properly: '%s'", cause.getMessage()),
                "This is likely caused by corrupted resources and there is nothing you can do about it. " +
                        "Please submit an issue to our tracker at " +
                        "`https://github.com/TheJacksonLaboratory/svann/issues` to get help.", cause);
    }
}
