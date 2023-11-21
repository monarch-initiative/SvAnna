package org.monarchinitiative.svanna.io.service;

import org.monarchinitiative.svanna.core.service.GeneService;
import org.monarchinitiative.svanna.core.service.QueryResult;
import org.monarchinitiative.svanna.io.service.jannovar.IntervalArray;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.monarchinitiative.sgenes.io.GeneParser;
import org.monarchinitiative.sgenes.io.GeneParserFactory;
import org.monarchinitiative.sgenes.io.SerializationFormat;
import org.monarchinitiative.sgenes.model.Gene;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class SilentGenesGeneService implements GeneService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SilentGenesGeneService.class);

    private final Map<Integer, IntervalArray<Gene>> chromosomeMap;

    private final Map<TermId, List<Gene>> geneByHgncId;

    public static SilentGenesGeneService of(GenomicAssembly assembly, Path silentGenesJsonPath) throws IOException {
        GeneParserFactory factory = GeneParserFactory.of(assembly);
        GeneParser geneParser = factory.forFormat(SerializationFormat.JSON);
        List<Gene> genes;
        try (InputStream is = openForReading(silentGenesJsonPath)) {
            genes = List.copyOf(geneParser.read(is));
        }
        // We group into a list of genes, because genes in pseudo-autosomal regions can be on 2 contigs,
        // hence two genes for a single HGNC id.
        // We get optional without checking because HGNC ID presence is checked in the stream filter.
        //noinspection OptionalGetWithoutIsPresent
        Map<TermId, List<Gene>> geneByHgncId = genes.stream()
                .filter(g -> g.id().hgncId().isPresent())
                .collect(Collectors.groupingBy(gene -> TermId.of(gene.id().hgncId().get()), Collectors.toUnmodifiableList()));

        Map<Integer, Set<Gene>> geneByContig = geneByHgncId.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(Gene::contigId, Collectors.toUnmodifiableSet()));

        Map<Integer, IntervalArray<Gene>> intervalArrayMap = new HashMap<>();
        for (int contig : geneByContig.keySet()) {
            Set<Gene> genesOnContig = geneByContig.get(contig);
            IntervalArray<Gene> intervalArray = new IntervalArray<>(genesOnContig, GeneEndExtractor.instance());
            intervalArrayMap.put(contig, intervalArray);
        }

        return new SilentGenesGeneService(Map.copyOf(intervalArrayMap), geneByHgncId);
    }

    private static InputStream openForReading(Path silentGenesJsonPath) throws IOException {
        if (silentGenesJsonPath.toFile().getName().endsWith(".gz")) {
            LOGGER.debug("Assuming the file is gzipped");
            return new BufferedInputStream(new GZIPInputStream(Files.newInputStream(silentGenesJsonPath)));
        } else {
            return new BufferedInputStream(Files.newInputStream(silentGenesJsonPath));
        }
    }

    private SilentGenesGeneService(Map<Integer, IntervalArray<Gene>> chromosomeMap, Map<TermId, List<Gene>> geneByHgncId) {
        this.chromosomeMap = chromosomeMap;
        this.geneByHgncId = geneByHgncId;
    }

    @Override
    public List<Gene> byHgncId(TermId hgncId) {
        return geneByHgncId.get(hgncId);
    }

    @Override
    public QueryResult<Gene> overlappingGenes(GenomicRegion query) {
        IntervalArray<Gene> array = chromosomeMap.get(query.contigId());
        if (array == null) {
            LOGGER.debug("Unknown contig ID {} for query {}:{}:{}", query.contigId(), query.contigName(), query.start(), query.end());
            return QueryResult.empty();
        }
        IntervalArray<Gene>.QueryResult result = query.length() == 0
                ? array.findOverlappingWithPoint(query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                : array.findOverlappingWithInterval(query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()), query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));

        return QueryResult.of(result.getEntries(), result.getLeft(), result.getRight());
    }
}
