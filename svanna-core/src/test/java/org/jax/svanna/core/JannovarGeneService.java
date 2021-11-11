package org.jax.svanna.core;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import org.jax.svanna.core.service.GeneService;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.Strand;
import xyz.ielis.silent.genes.jannovar.JannovarParser;
import xyz.ielis.silent.genes.model.Gene;
import xyz.ielis.silent.genes.model.Transcript;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides genes based on transcripts defined in given {@link JannovarData}. The transcripts are remapped into the
 * coordinate system of given {@link GenomicAssembly} using contig names.
 * <p>
 * Note that NO checking is performed to ensure that the transcript coordinates actually make sense in the given
 * assembly, it is the user's responsibility to provide proper assembly and transcript database path.
 */
// TODO - the class might be moved outside of svanna-core
public class JannovarGeneService implements GeneService {

    private final Map<Integer, IntervalArray<Gene>> chromosomeMap;

    private final Map<String, Gene> geneBySymbol;

    public static JannovarGeneService of(GenomicAssembly assembly, Path jannovarTranscriptDbPath) {
        JannovarParser parser = JannovarParser.of(jannovarTranscriptDbPath, assembly);
        Map<String, Gene> geneBySymbol = parser.stream()
                .filter(g -> g.transcripts().anyMatch(Transcript::isCoding))
                .collect(Collectors.toUnmodifiableMap(Gene::symbol, Function.identity()));

        // build interval arrays
        GeneEndExtractor endExtractor = new GeneEndExtractor();
        Map<Integer, IntervalArray<Gene>> intervalArrayMap = new HashMap<>();
        Map<Integer, Set<Gene>> geneByContig = geneBySymbol.values().stream()
                .collect(Collectors.groupingBy(Gene::contigId, Collectors.toUnmodifiableSet()));
        for (int contig : geneByContig.keySet()) {
            Set<Gene> genesOnContig = geneByContig.get(contig);
            IntervalArray<Gene> intervalArray = new IntervalArray<>(genesOnContig, endExtractor);
            intervalArrayMap.put(contig, intervalArray);
        }

        return new JannovarGeneService(intervalArrayMap, geneBySymbol);
    }

    private JannovarGeneService(Map<Integer, IntervalArray<Gene>> chromosomeMap,
                                Map<String, Gene> geneBySymbol) {
        this.chromosomeMap = Objects.requireNonNull(chromosomeMap, "Chromosome map must not be null");
        this.geneBySymbol = Objects.requireNonNull(geneBySymbol, "Gene by symbol must not be null");
    }

    @Override
    public Map<Integer, IntervalArray<Gene>> getChromosomeMap() {
        return chromosomeMap;
    }

    @Override
    public Gene bySymbol(String symbol) {
        return geneBySymbol.get(symbol);
    }

    private static class GeneEndExtractor implements IntervalEndExtractor<Gene> {

        @Override
        public int getBegin(Gene gene) {
            return gene.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        }

        @Override
        public int getEnd(Gene gene) {
            return gene.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        }
    }


}
