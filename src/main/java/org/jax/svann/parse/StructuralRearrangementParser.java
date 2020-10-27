package org.jax.svann.parse;

import org.jax.svann.reference.SequenceRearrangement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public interface StructuralRearrangementParser {

    Collection<SequenceRearrangement> parseFile(Path filePath) throws IOException;
}
