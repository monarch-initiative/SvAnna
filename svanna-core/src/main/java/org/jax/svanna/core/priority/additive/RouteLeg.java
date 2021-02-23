package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.*;

public interface RouteLeg extends GenomicRegion {

    static RouteLeg of(String id, Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, int contribution) {
        return RouteLegDefault.of(id, contig, strand, coordinateSystem, startPosition, endPosition, contribution);
    }

    String id();

    int contribution();
}
