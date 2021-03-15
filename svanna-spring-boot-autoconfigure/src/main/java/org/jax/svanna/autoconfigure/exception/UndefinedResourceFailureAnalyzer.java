package org.jax.svanna.autoconfigure.exception;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * @author Daniel Danis
 */
public class UndefinedResourceFailureAnalyzer extends AbstractFailureAnalyzer<UndefinedResourceException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, UndefinedResourceException cause) {

        return new FailureAnalysis(String.format("SvAnna could not be auto-configured properly: '%s'", cause.getMessage()),
                "You need to define the property 'svanna.data-directory'. " +
                        "You can include them in your application.properties or supply your application with '--svanna.data-directory=', etc.. as a startup argument.",
                cause);
    }
}
