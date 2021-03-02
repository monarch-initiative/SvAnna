package org.jax.svanna.core.reference.transcripts;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.GeneDefault;
import org.jax.svanna.core.reference.GeneService;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides genes based on transcripts defined in given {@link JannovarData}. The transcripts are remapped into the
 * coordinate system of given {@link GenomicAssembly} using contig names.
 * <p>
 * Note that no checking is performed to ensure that the transcript coordinates actually make sense in the given
 * assembly, it is the user's responsibility to provide suitable inputs.
 */
public class JannovarGeneService implements GeneService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JannovarGeneService.class);

    private final Map<Integer, IntervalArray<Gene>> chromosomeMap;

    private final Map<String, Gene> geneBySymbol;

    /**
     * Parse and remap the transcripts from the provided <code>databases</code> into coordinates of given <code>assembly</code>.
     *
     * @param assembly  genome assembly
     * @param databases Jannovar transcript databases
     * @return Jannovar transcript service
     */
    public static JannovarGeneService of(GenomicAssembly assembly, JannovarData... databases) {
        JannovarTxMapper remapper = new JannovarTxMapper(assembly);
        Map<String, GeneDefault.Builder> geneBuilder = new HashMap<>();

        // remap all the transcripts into SvAnna's coordinate system
        for (JannovarData database : databases) {
            ImmutableMap<String, TranscriptModel> tmByAccession = database.getTmByAccession();
            for (String accessionId : tmByAccession.keySet()) {
                TranscriptModel tm = tmByAccession.get(accessionId);
                Optional<Transcript> txOpt = remapper.remap(tm);
                txOpt.ifPresent(tx -> {
                    if (!geneBuilder.containsKey(tm.getGeneSymbol())) {
                        geneBuilder.put(tm.getGeneSymbol(), GeneDefault.builder()
                                .accessionId(TermId.of("HGNC", tm.getGeneID()))
                                .hgvsSymbol(TermId.of("HGVS", tm.getGeneSymbol())));
                    }
                    geneBuilder.get(tm.getGeneSymbol()).addTranscript(tx);
                });
            }
        }

        Map<String, Gene> geneBySymbol = geneBuilder.entrySet().stream()
                .map(buildGeneIfPossible())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(gene -> gene.hgvsName().getValue(), Function.identity()));

        Map<Integer, Set<Gene>> geneByContig = geneBySymbol.values().stream()
                .collect(Collectors.groupingBy(Gene::contigId, Collectors.toSet()));

        // build interval arrays
        GeneEndExtractor endExtractor = new GeneEndExtractor();
        Map<Integer, IntervalArray<Gene>> intervalArrayMap = new HashMap<>();
        for (int contig : geneByContig.keySet()) {
            Set<Gene> genesOnContig = geneByContig.get(contig);
            IntervalArray<Gene> intervalArray = new IntervalArray<>(genesOnContig, endExtractor);
            intervalArrayMap.put(contig, intervalArray);
        }

        return new JannovarGeneService(intervalArrayMap, geneBySymbol);

    }

    private JannovarGeneService(Map<Integer, IntervalArray<Gene>> chromosomeMap,
                                Map<String, Gene> geneBySymbol) {
        this.chromosomeMap = Map.copyOf(chromosomeMap);
        this.geneBySymbol = Map.copyOf(geneBySymbol);
    }

    @Override
    public Map<Integer, IntervalArray<Gene>> getChromosomeMap() {
        return chromosomeMap;
    }

    @Override
    public Gene bySymbol(String symbol) {
        return geneBySymbol.get(symbol);
    }

    private static Function<Map.Entry<String, GeneDefault.Builder>, Optional<Gene>> buildGeneIfPossible() {
        return entry -> {
            Gene gene;
            try {
                gene = entry.getValue().build();
            } catch (Exception e) {
                LogUtils.logWarn(LOGGER, "Unable to remap gene {}: {}", entry.getKey(), e.getMessage());
                return Optional.empty();
            }
            return Optional.of(gene);
        };
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
