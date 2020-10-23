package org.jax.svann.parse;

import java.io.IOException;
import java.nio.file.Path;

public interface StructuralVariantParser {

    ParseResult parseFile(Path filePath) throws IOException ;

}
