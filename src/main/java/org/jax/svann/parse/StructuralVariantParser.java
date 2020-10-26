package org.jax.svann.parse;

import java.io.IOException;
import java.nio.file.Path;

/**
 * The classes that implement this interface load a file with structural variants into the domain model of this
 * application.
 */
public interface StructuralVariantParser {

    ParseResult parseFile(Path filePath) throws IOException ;

}
