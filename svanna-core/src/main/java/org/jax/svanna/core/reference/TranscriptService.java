package org.jax.svanna.core.reference;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;

import java.util.Map;
import java.util.Set;

public interface TranscriptService {

    Map<Integer, IntervalArray<Transcript>> getChromosomeMap();

    Map<String, Transcript> getTxByAccessionMap();

    Map<String, Set<Transcript>> getTxBySymbolMap();

}
