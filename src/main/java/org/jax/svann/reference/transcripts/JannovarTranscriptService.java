package org.jax.svann.reference.transcripts;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.reference.CoordinateSystem;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.GenomeAssembly;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class provides transcripts defined in given {@link JannovarData}. The transcripts are remapped into the
 * coordinate system of given {@link GenomeAssembly} using contig names.
 * <p>
 * Note that no checking is performed to ensure that the transcript coordinates actually make sense in the given
 * assembly, it is the user's responsibility to provide suitable inputs.
 */
public class JannovarTranscriptService implements TranscriptService {

    private final Map<Integer, IntervalArray<SvAnnTxModel>> chromosomeMap;

    private final Map<String, SvAnnTxModel> txByAccession;

    private JannovarTranscriptService(Map<Integer, IntervalArray<SvAnnTxModel>> chromosomeMap,
                                      Map<String, SvAnnTxModel> txByAccession) {
        this.chromosomeMap = Map.copyOf(chromosomeMap);
        this.txByAccession = Map.copyOf(txByAccession);
    }

    /**
     * Parse and remap the transcripts from the provided <code>databases</code> into coordinates of given <code>assembly</code>.
     *
     * @param assembly  genome assembly
     * @param databases Jannovar transcript databases
     * @return Jannovar transcript service
     */
    public static JannovarTranscriptService of(GenomeAssembly assembly, JannovarData... databases) {
        JannovarTxMapper remapper = new JannovarTxMapper(assembly);
        Map<Integer, List<SvAnnTxModel>> map = new HashMap<>();
        Map<String, SvAnnTxModel> txByAccession = new HashMap<>();

        // remap all the transcripts into SvAnn's coordinate system
        for (JannovarData database : databases) {
            ImmutableMap<String, TranscriptModel> tmByAccession = database.getTmByAccession();
            for (String accessionId : tmByAccession.keySet()) {
                TranscriptModel tm = tmByAccession.get(accessionId);
                Optional<SvAnnTxModel> txOpt = remapper.remap(tm);
                txOpt.ifPresent(tx -> {
                    int contigId = tx.getContigId();
                    if (!map.containsKey(contigId)) {
                        map.put(contigId, new ArrayList<>());
                    }
                    map.get(contigId).add(tx);
                    txByAccession.put(tx.getAccession(), tx);
                });
            }
        }

        // build interval arrays
        SvAnnTxModelExtractor endExtractor = new SvAnnTxModelExtractor();
        Map<Integer, IntervalArray<SvAnnTxModel>> intervalArrayMap = map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new IntervalArray<>(e.getValue(), endExtractor)));
        return new JannovarTranscriptService(intervalArrayMap, txByAccession);

    }

    @Override
    public Map<Integer, IntervalArray<SvAnnTxModel>> getChromosomeMap() {
        return chromosomeMap;
    }

    @Override
    public Map<String, SvAnnTxModel> getTxByAccessionMap() {
        return txByAccession;
    }

    private static class SvAnnTxModelExtractor implements IntervalEndExtractor<SvAnnTxModel> {

        @Override
        public int getBegin(SvAnnTxModel svAnnTxModel) {
            GenomicPosition startOnFwd = svAnnTxModel.getStart().withStrand(Strand.FWD);
            if (startOnFwd.getCoordinateSystem().equals(CoordinateSystem.ONE_BASED)) {
                // convert to zero-based
                return startOnFwd.getPosition() - 1;
            } else {
                return startOnFwd.getPosition();
            }
        }

        @Override
        public int getEnd(SvAnnTxModel svAnnTxModel) {
            // this position should be correct all the time
            return svAnnTxModel.getEnd().withStrand(Strand.FWD).getPosition();
        }
    }


}
