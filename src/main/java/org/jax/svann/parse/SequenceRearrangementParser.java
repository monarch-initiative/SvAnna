package org.jax.svann.parse;

import org.jax.svann.reference.SequenceRearrangement;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SequenceRearrangementParser<T extends SequenceRearrangement> {

    List<T> parseFile(Path filePath) throws IOException;
}
