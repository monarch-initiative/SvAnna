package org.jax.svanna.io.parse;

import org.jax.svanna.core.reference.SvannaVariant;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface VariantParser<T extends SvannaVariant> {

    /**
     * Obtain a stream of variants read from given <code>filePath</code>.
     * <p>
     * Note that the stream should be closed.
     *
     * @param filePath path to variant file
     * @return stream of variants
     * @throws IOException in case of I/O errors
     */
    Stream<T> createVariantAlleles(Path filePath) throws IOException;


    default List<T> createVariantAlleleList(Path filePath) throws IOException {
        try (Stream<T> alleles = createVariantAlleles(filePath)) {
            return alleles.collect(Collectors.toList());
        }
    }
}
