package org.jax.svanna.core.landscape;

import org.monarchinitiative.svart.*;

import java.util.Objects;

public class TadBoundaryDefault extends BaseGenomicRegion<TadBoundaryDefault> implements TadBoundary {

    private final String id;

    private final float stability;

    protected TadBoundaryDefault(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, String id, float stability) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.id = id;
        this.stability = stability;
    }

    public static TadBoundaryDefault of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition,
                                        String id, float stability) {
        return new TadBoundaryDefault(contig, strand, coordinateSystem, startPosition, endPosition, id, stability);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public float stability() {
        return stability;
    }

    @Override
    protected TadBoundaryDefault newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new TadBoundaryDefault(contig, strand, coordinateSystem, startPosition, endPosition, id, stability);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TadBoundaryDefault that = (TadBoundaryDefault) o;
        return Float.compare(that.stability, stability) == 0 && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, stability);
    }

    @Override
    public String toString() {
        return "TadBoundaryDefault{" +
                "id=" + id +
                ", region=" + super.toString() +
                ", stability=" + stability +
                '}';
    }
}
