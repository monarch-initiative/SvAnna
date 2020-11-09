package org.jax.svann.reference.transcripts;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;

import java.util.Map;

public interface TranscriptService {

    Map<Integer, IntervalArray<SvAnnTxModel>> getChromosomeMap();

    Map<String, SvAnnTxModel> getTxByAccessionMap();
}
