package org.jax.svanna.io.service;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jax.svanna.core.service.GeneService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicAssembly;
import xyz.ielis.silent.genes.io.GeneParser;
import xyz.ielis.silent.genes.io.GeneParserFactory;
import xyz.ielis.silent.genes.io.SerializationFormat;
import xyz.ielis.silent.genes.model.Gene;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SilentGenesGeneService implements GeneService {

    private final Map<Integer, IntervalArray<Gene>> chromosomeMap;

    private final Map<TermId, Gene> geneByTermId;

    public static SilentGenesGeneService of(GenomicAssembly assembly, Path silentGenesJsonPath) throws IOException {
        GeneParserFactory factory = GeneParserFactory.of(assembly);
        GeneParser geneParser = factory.forFormat(SerializationFormat.JSON);
        List<? extends Gene> genes;
        try (InputStream is = openForReading(silentGenesJsonPath)) {
            genes = geneParser.read(is);
        }
        // We check for HGNC ID presence in the filter clause
        //noinspection OptionalGetWithoutIsPresent
        Map<TermId, Gene> geneBySymbol = genes.stream()
                .filter(g -> g.id().hgncId().isPresent())
                .collect(Collectors.toUnmodifiableMap(gene -> TermId.of(gene.id().hgncId().get()), Function.identity()));

        Map<Integer, Set<Gene>> geneByContig = geneBySymbol.values().stream()
                .collect(Collectors.groupingBy(Gene::contigId, Collectors.toUnmodifiableSet()));

        Map<Integer, IntervalArray<Gene>> intervalArrayMap = new HashMap<>();
        for (int contig : geneByContig.keySet()) {
            Set<Gene> genesOnContig = geneByContig.get(contig);
            IntervalArray<Gene> intervalArray = new IntervalArray<>(genesOnContig, GeneEndExtractor.instance());
            intervalArrayMap.put(contig, intervalArray);
        }

        return new SilentGenesGeneService(Map.copyOf(intervalArrayMap), geneBySymbol);
    }

    private static InputStream openForReading(Path silentGenesJsonPath) throws IOException {
        if (silentGenesJsonPath.toFile().getName().endsWith(".gz")) {
            LOGGER.debug("Assuming the file is gzipped");
            return new BufferedInputStream(new GzipCompressorInputStream(Files.newInputStream(silentGenesJsonPath)));
        } else {
            return new BufferedInputStream(Files.newInputStream(silentGenesJsonPath));
        }
    }

    private SilentGenesGeneService(Map<Integer, IntervalArray<Gene>> chromosomeMap, Map<TermId, Gene> geneByTermId) {
        this.chromosomeMap = chromosomeMap;
        this.geneByTermId = geneByTermId;
    }

    @Override
    public Map<Integer, IntervalArray<Gene>> getChromosomeMap() {
        return chromosomeMap;
    }

    @Override
    public Optional<Gene> byHgncId(TermId hgncId) {
        return Optional.ofNullable(geneByTermId.get(hgncId));
    }

}
