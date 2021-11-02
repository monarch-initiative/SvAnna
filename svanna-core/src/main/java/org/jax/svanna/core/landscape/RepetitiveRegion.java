package org.jax.svanna.core.landscape;

import org.monarchinitiative.svart.*;

import java.util.Objects;

public class RepetitiveRegion extends BaseGenomicRegion<RepetitiveRegion> {

    private final RepeatFamily repeatFamily;

    public static RepetitiveRegion of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, int startPosition, int endPosition, RepeatFamily repeatFamily) {
        return new RepetitiveRegion(contig, strand, Coordinates.of(coordinateSystem, startPosition, endPosition), repeatFamily);
    }

    public static RepetitiveRegion of(Contig contig, Strand strand, Coordinates coordinates, RepeatFamily repeatFamily) {
        return new RepetitiveRegion(contig, strand, coordinates, repeatFamily);
    }

    private RepetitiveRegion(Contig contig, Strand strand, Coordinates coordinates, RepeatFamily repeatFamily) {
        super(contig, strand, coordinates);
        this.repeatFamily = repeatFamily;
    }

    public RepeatFamily repeatFamily() {
        return repeatFamily;
    }

    @Override
    protected RepetitiveRegion newRegionInstance(Contig contig, Strand strand, Coordinates coordinates) {
        return new RepetitiveRegion(contig, strand, coordinates, repeatFamily);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RepetitiveRegion that = (RepetitiveRegion) o;
        return repeatFamily == that.repeatFamily;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), repeatFamily);
    }

    @Override
    public String toString() {
        return "RepetitiveRegion{" +
                "region=" + super.toString() +
                ", repeatFamily=" + repeatFamily +
                '}';
    }
}
