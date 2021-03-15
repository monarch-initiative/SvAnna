package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.*;

public interface Segment extends GenomicRegion {

    static Segment of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, String id, Event event, int copies) {
        return SegmentSimple.of(contig, strand, coordinateSystem, startPosition, endPosition, id, event, copies);
    }

    static Segment insertion(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, String id, int length) {
        return SegmentInsertion.of(contig, strand, coordinateSystem, startPosition, endPosition, id, length);
    }

    String id();

    Event event();

    /**
     * @return number of consecutive segment copies in {@link Route}.
     */
    int copies();

    /**
     * @return number of bases the segment contributes into the overall length of a {@link Route}
     */
    default int contributingBases() {
        return copies() * length();
    }

    @Override
    Segment withCoordinateSystem(CoordinateSystem coordinateSystem);

    @Override
    Segment withStrand(Strand other);

}
