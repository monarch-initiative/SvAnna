package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.*;

public interface Segment extends GenomicRegion {

    static Segment of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end, String id, Event event, int copies) {
        return of(contig, strand, Coordinates.of(coordinateSystem, start, end), id, event, copies);
    }

    static Segment of(Contig contig, Strand strand, Coordinates coordinates, String id, Event event, int copies) {
        return SegmentSimple.of(contig, strand, coordinates, id, event, copies);
    }

    static Segment insertion(Contig contig, Strand strand, CoordinateSystem coordinateSystem, int startPosition, int endPosition, String id, int length) {
        return insertion(contig, strand, Coordinates.of(coordinateSystem, startPosition, endPosition), id, length);
    }

    static Segment insertion(Contig contig, Strand strand, Coordinates coordinates, String id, int length) {
        return SegmentInsertion.of(contig, strand, coordinates, id, length);
    }

    String id();

    Event event();

    /**
     * @return number of consecutive segment copies in {@link Segment}.
     */
    int copies();

    /**
     * @return number of bases the segment contributes into the overall length of a {@link Segment}
     */
    default int contributingBases() {
        return copies() * length();
    }

    @Override
    Segment withCoordinateSystem(CoordinateSystem coordinateSystem);

    @Override
    Segment withStrand(Strand other);

}
