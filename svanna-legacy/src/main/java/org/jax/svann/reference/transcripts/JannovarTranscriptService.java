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

    private final Map<String, Set<SvAnnTxModel>> txBySymbol;

    private JannovarTranscriptService(Map<Integer, IntervalArray<SvAnnTxModel>> chromosomeMap,
                                      Map<String, SvAnnTxModel> txByAccession,
                                      Map<String, Set<SvAnnTxModel>> txBySymbol) {
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
    public static JannovarTranscriptService of(GenomeAssembly assembly, JannovarData... databases) {
        JannovarTxMapper remapper = new JannovarTxMapper(assembly);
        Map<Integer, List<SvAnnTxModel>> map = new HashMap<>();
        Map<String, SvAnnTxModel> txByAccession = new HashMap<>();
        Map<String, Set<SvAnnTxModel>> txBySymbol = new HashMap<>();

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
                    if (!txBySymbol.containsKey(tx.getGeneSymbol())) {
                        txBySymbol.put(tx.getGeneSymbol(), new HashSet<>());
                    }
                    txBySymbol.get(tx.getGeneSymbol()).add(tx);
                });
            }
        }

        // build interval arrays
        SvAnnTxModelExtractor endExtractor = new SvAnnTxModelExtractor();
        Map<Integer, IntervalArray<SvAnnTxModel>> intervalArrayMap = new HashMap<>();
        for (int contig : map.keySet()) {
            List<SvAnnTxModel> transcripts = map.get(contig);
            IntervalArray<SvAnnTxModel> intervalArray = new IntervalArray<>(transcripts, endExtractor);
            intervalArrayMap.put(contig, intervalArray);
        }

        return new JannovarTranscriptService(intervalArrayMap, txByAccession, txBySymbol);

    }

    @Override
    public Map<Integer, IntervalArray<SvAnnTxModel>> getChromosomeMap() {
        return chromosomeMap;
    }

    @Override
    public Map<String, SvAnnTxModel> getTxByAccessionMap() {
        return txByAccession;
    }

    @Override
    public Map<String, Set<SvAnnTxModel>> getTxBySymbolMap() {
        return txBySymbol;
    }

    private static class SvAnnTxModelExtractor implements IntervalEndExtractor<SvAnnTxModel> {

        @Override
        public int getBegin(SvAnnTxModel svAnnTxModel) {
            GenomicPosition startOnFwd = svAnnTxModel.withStrand(Strand.FWD).getStart();
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
            return svAnnTxModel.withStrand(Strand.FWD).getEnd().getPosition();
        }
    }


}
