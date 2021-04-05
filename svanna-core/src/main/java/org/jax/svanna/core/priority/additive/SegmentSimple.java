package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.*;

import java.text.NumberFormat;
import java.util.Objects;

class SegmentSimple extends BaseGenomicRegion<SegmentSimple> implements Segment {

    private static final NumberFormat NF = NumberFormat.getInstance();

    protected final String id;
    protected final Event event;
    protected final int copies;

    static SegmentSimple of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, String id, Event event, int copies) {
        return new SegmentSimple(contig, strand, coordinateSystem, startPosition, endPosition, id, event, copies);
    }

    protected SegmentSimple(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, String id, Event event, int copies) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.id = id;
        this.event = event;
        this.copies = copies;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public Event event() {
        return event;
    }

    @Override
    public int copies() {
        return copies;
    }

    @Override
    protected SegmentSimple newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new SegmentSimple(contig, strand, coordinateSystem, startPosition, endPosition, id, event, copies);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SegmentSimple that = (SegmentSimple) o;
        return copies == that.copies && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, copies);
    }

    @Override
    public String toString() {
        return "SegmentSimple{" +
                contigId() + ':' + NF.format(start()) + '-' + NF.format(end()) + '(' + strand() + ')' +
                "id='" + id + '\'' +
                ", copies=" + copies +
                '}';
    }
}
