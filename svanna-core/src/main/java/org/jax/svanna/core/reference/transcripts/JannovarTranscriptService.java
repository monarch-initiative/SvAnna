package org.jax.svanna.core.reference.transcripts;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svanna.core.reference.Transcript;
import org.jax.svanna.core.reference.TranscriptService;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.Strand;

import java.util.*;

/**
 * This class provides transcripts defined in given {@link JannovarData}. The transcripts are remapped into the
 * coordinate system of given {@link GenomicAssembly} using contig names.
 * <p>
 * Note that no checking is performed to ensure that the transcript coordinates actually make sense in the given
 * assembly, it is the user's responsibility to provide suitable inputs.
 */
public class JannovarTranscriptService implements TranscriptService {

    private final Map<Integer, IntervalArray<Transcript>> chromosomeMap;

    private final Map<String, Transcript> txByAccession;

    private final Map<String, Set<Transcript>> txBySymbol;

    private JannovarTranscriptService(Map<Integer, IntervalArray<Transcript>> chromosomeMap,
                                      Map<String, Transcript> txByAccession,
                                      Map<String, Set<Transcript>> txBySymbol) {
        this.chromosomeMap = Map.copyOf(chromosomeMap);
        this.txByAccession = Map.copyOf(txByAccession);
        this.txBySymbol = txBySymbol;
    }

    /**
     * Parse and remap the transcripts from the provided <code>databases</code> into coordinates of given <code>assembly</code>.
     *
     * @param assembly  genome assembly
     * @param databases Jannovar transcript databases
     * @return Jannovar transcript service
     */
    public static JannovarTranscriptService of(GenomicAssembly assembly, JannovarData... databases) {
        JannovarTxMapper remapper = new JannovarTxMapper(assembly);
        Map<Integer, List<Transcript>> map = new HashMap<>();
        Map<String, Transcript> txByAccession = new HashMap<>();
        Map<String, Set<Transcript>> txBySymbol = new HashMap<>();

        // remap all the transcripts into SvAnna's coordinate system
        for (JannovarData database : databases) {
            ImmutableMap<String, TranscriptModel> tmByAccession = database.getTmByAccession();
            for (String accessionId : tmByAccession.keySet()) {
                TranscriptModel tm = tmByAccession.get(accessionId);
                Optional<Transcript> txOpt = remapper.remap(tm);
                txOpt.ifPresent(tx -> {
                    int contigId = tx.contigId();
                    if (!map.containsKey(contigId)) {
                        map.put(contigId, new ArrayList<>());
                    }
                    map.get(contigId).add(tx);
                    txByAccession.put(tx.accessionId(), tx);
                    if (!txBySymbol.containsKey(tm.getGeneSymbol())) {
                        txBySymbol.put(tm.getGeneSymbol(), new HashSet<>());
                    }
                    txBySymbol.get(tm.getGeneSymbol()).add(tx);
                });
            }
        }

        // build interval arrays
        TranscriptEndExtractor endExtractor = new TranscriptEndExtractor();
        Map<Integer, IntervalArray<Transcript>> intervalArrayMap = new HashMap<>();
        for (int contig : map.keySet()) {
            List<Transcript> transcripts = map.get(contig);
            IntervalArray<Transcript> intervalArray = new IntervalArray<>(transcripts, endExtractor);
            intervalArrayMap.put(contig, intervalArray);
        }

        return new JannovarTranscriptService(intervalArrayMap, txByAccession, txBySymbol);

    }

    @Override
    public Map<Integer, IntervalArray<Transcript>> getChromosomeMap() {
        return chromosomeMap;
    }

    @Override
    public Map<String, Transcript> getTxByAccessionMap() {
        return txByAccession;
    }

    @Override
    public Map<String, Set<Transcript>> getTxBySymbolMap() {
        return txBySymbol;
    }

    private static class TranscriptEndExtractor implements IntervalEndExtractor<Transcript> {

        @Override
        public int getBegin(Transcript transcript) {
            return transcript.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        }

        @Override
        public int getEnd(Transcript transcript) {
            return transcript.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        }
    }


}
