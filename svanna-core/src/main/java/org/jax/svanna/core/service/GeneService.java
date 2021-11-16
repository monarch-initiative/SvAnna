package org.jax.svanna.core.service;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import org.jax.svanna.core.LogUtils;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ielis.silent.genes.model.Gene;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GeneService {

    Logger LOGGER = LoggerFactory.getLogger(GeneService.class);

    Map<Integer, IntervalArray<Gene>> getChromosomeMap();

    Optional<Gene> byHgncId(TermId hgncId);

    default List<Gene> overlappingGenes(GenomicRegion query) {
        IntervalArray<Gene> array = getChromosomeMap().get(query.contigId());
        if (array == null) {
            LogUtils.logDebug(LOGGER, "Unknown contig ID {} for query {}:{}:{}", query.contigId(), query.contigName(), query.start(), query.end());
            return List.of();
        }
        IntervalArray<Gene>.QueryResult result = query.length() == 0
                ? array.findOverlappingWithPoint(query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                : array.findOverlappingWithInterval(query.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()), query.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()));
        return result.getEntries();
    }
}
