package org.jax.svanna.cli.writer;

import org.jax.svanna.core.reference.SvannaVariant;

import java.io.IOException;

/**
 * Implementors write {@link SvannaVariant}s in different formats.
 *
 * @author Daniel Danis
 */
public interface ResultWriter {

    void write(AnalysisResults analysisResults, OutputOptions outputOptions) throws IOException;

}
