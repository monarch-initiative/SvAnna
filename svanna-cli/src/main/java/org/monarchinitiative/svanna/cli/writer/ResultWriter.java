package org.monarchinitiative.svanna.cli.writer;

import org.monarchinitiative.svanna.core.reference.SvannaVariant;

import java.io.IOException;

/**
 * Implementors write {@link SvannaVariant}s in different formats.
 *
 * @author Daniel Danis
 */
public interface ResultWriter {

    void write(AnalysisResults analysisResults, OutputOptions outputOptions) throws IOException;

}
