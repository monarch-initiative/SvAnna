package org.jax.svanna.core.annotation;

import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.impl.intervals.IntervalEndExtractor;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public interface AnnotationDao<T extends GenomicRegion> {

    List<T> getAllItems();

    List<T> getOverlapping(GenomicRegion query);

    default Map<Integer, IntervalArray<T>> getChromosomeMap() {
        Map<Integer, List<T>> annotationsByContigId = getAllItems().stream().collect(groupingBy(GenomicRegion::contigId));
        return annotationsByContigId.entrySet().stream()
                .collect(toMap(Map.Entry::getKey, entry -> new IntervalArray<>(entry.getValue(), new DefaultEndExtractor<>())));
    }

    class DefaultEndExtractor<T extends GenomicRegion> implements IntervalEndExtractor<T> {

        @Override
        public int getBegin(T x) {
            return x.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        }

        @Override
        public int getEnd(T x) {
            return x.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        }
    }
}
