package org.jax.svanna.ingest.parse;

import org.monarchinitiative.svart.GenomicRegion;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IngestRecordParser<T extends GenomicRegion> {

    Stream<T> parse() throws IOException;

    default List<T> parseToList() throws IOException {
        try (Stream<T> parse = parse()) {
            return parse.collect(Collectors.toList());
        }
    }
}
