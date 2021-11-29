package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;

import java.util.Objects;

/**
 * We need to have this segment class to be able to represent an insertion of unknown sequence that has contig length 0,
 * but still it inserts some known number of bases into a contig. The number of inserted bases is returned
 * by {@link #length()}.
 */
class SegmentInsertion extends SegmentSimple {

    // This is 1 by definition
    private static final int N_COPIES = 1;

    private static final Event INSERTION_EVENT = Event.INSERTION;

    private final int length;

    static SegmentInsertion of(Contig contig, Strand strand, Coordinates coordinates, String id, int length) {
        return new SegmentInsertion(contig, strand, coordinates, id, length);
    }

    protected SegmentInsertion(Contig contig, Strand strand, Coordinates coordinates, String id, int length) {
        super(contig, strand, coordinates, id, INSERTION_EVENT, N_COPIES);
        this.length = length;
    }

    @Override
    protected SegmentInsertion newRegionInstance(Contig contig, Strand strand, Coordinates coordinates) {
        return new SegmentInsertion(contig, strand, coordinates, id, length);
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SegmentInsertion that = (SegmentInsertion) o;
        return length == that.length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), length);
    }

    @Override
    public String toString() {
        return "SegmentInsertion{" +
                "length=" + length +
                '}';
    }
}
