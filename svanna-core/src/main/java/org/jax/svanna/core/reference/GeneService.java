package org.jax.svanna.core.reference;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.List;
import java.util.Map;

public interface GeneService {

    Map<Integer, IntervalArray<Gene>> getChromosomeMap();

    Gene bySymbol(String symbol);

    default List<Gene> overlappingGenes(GenomicRegion query) {
        IntervalArray<Gene> array = getChromosomeMap().get(query.contigId());
        IntervalArray<Gene>.QueryResult result =
                query.length() == 0
                        ? array.findOverlappingWithPoint(query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                        : array.findOverlappingWithInterval(query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()), query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
        return result.getEntries();
    }
}
