package org.jax.svanna.core.landscape;

import org.monarchinitiative.svart.*;

import java.util.Objects;

public class TadBoundaryDefault extends BaseGenomicRegion<TadBoundaryDefault> implements TadBoundary {

    private final String id;

    private final float stability;

    private TadBoundaryDefault(Contig contig, Strand strand, Coordinates coordinates, String id, float stability) {
        super(contig, strand, coordinates);
        this.id = id;
        this.stability = stability;
    }

    public static TadBoundaryDefault of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, int start, int end,
                                        String id, float stability) {
        return of(contig, strand, Coordinates.of(coordinateSystem, start, end), id, stability);
    }

    public static TadBoundaryDefault of(Contig contig, Strand strand, Coordinates coordinates, String id, float stability) {
        return new TadBoundaryDefault(contig, strand, coordinates, id, stability);
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
    protected TadBoundaryDefault newRegionInstance(Contig contig, Strand strand, Coordinates coordinates) {
        return new TadBoundaryDefault(contig, strand, coordinates, id, stability);
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
