package org.jax.svanna.core;

import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.core.service.QueryResult;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.svart.GenomicRegion;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

class SilentGenesGeneService implements GeneService {

    private final Map<Integer, List<Gene>> chromosomeMap;

    private final Map<TermId, Gene> genesByTermId;

    static SilentGenesGeneService of(GenomicAssembly assembly, Path silentGenesJsonPath) throws IOException {
        GeneParserFactory factory = GeneParserFactory.of(assembly);
        GeneParser geneParser = factory.forFormat(SerializationFormat.JSON);
        List<? extends Gene> genes;
        try (InputStream is = new BufferedInputStream(new GZIPInputStream(Files.newInputStream(silentGenesJsonPath)))) {
            genes = geneParser.read(is);
        }
        // We check for HGNC ID presence in the filter clause
        //noinspection OptionalGetWithoutIsPresent
        Map<TermId, Gene> geneBySymbol = genes.stream()
                .filter(g -> g.id().hgncId().isPresent())
                .collect(Collectors.toUnmodifiableMap(gene -> TermId.of(gene.id().hgncId().get()), Function.identity()));

        Map<Integer, List<Gene>> geneByContig = geneBySymbol.values().stream()
                .collect(Collectors.groupingBy(Gene::contigId, Collectors.toUnmodifiableList()));

        return new SilentGenesGeneService(geneByContig, geneBySymbol);
    }

    private SilentGenesGeneService(Map<Integer, List<Gene>> chromosomeMap, Map<TermId, Gene> genesByTermId) {
        this.chromosomeMap = chromosomeMap;
        this.genesByTermId = genesByTermId;
    }

    @Override
    public List<Gene> byHgncId(TermId hgncId) {
        // this is a mock/test service, we always have only one gene per HGNC ID
        return List.of(genesByTermId.get(hgncId));
    }

    @Override
    public QueryResult<Gene> overlappingGenes(GenomicRegion query) {
        List<Gene> genesOnContig = chromosomeMap.get(query.contigId());
        if (genesOnContig == null)
            return QueryResult.empty();

        List<Gene> entries = genesOnContig.stream()
                .filter(g -> g.location().overlapsWith(query))
                .collect(Collectors.toList());

        // this should be correct, but I'm not 100% sure
        Gene upstream = genesOnContig.stream()
                .filter(g -> query.distanceTo(g.location()) < 0)
                .max((l, r) -> GenomicRegion.compare(l.location(), r.location()))
                .orElse(null);
        Gene downstream = genesOnContig.stream()
                .filter(g -> query.distanceTo(g.location()) > 0)
                .min((l, r) -> GenomicRegion.compare(l.location(), r.location()))
                .orElse(null);

        return QueryResult.of(entries, upstream, downstream);
    }

}
