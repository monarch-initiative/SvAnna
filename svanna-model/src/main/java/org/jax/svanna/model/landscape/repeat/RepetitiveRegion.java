package org.jax.svanna.model.landscape.repeat;

import org.jax.svanna.model.landscape.BaseLocated;
import org.monarchinitiative.svart.*;

import java.util.Objects;

public class RepetitiveRegion extends BaseLocated {

    private final RepeatFamily repeatFamily;

    public static RepetitiveRegion of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, int startPosition, int endPosition, RepeatFamily repeatFamily) {
        GenomicRegion location = GenomicRegion.of(contig, strand, coordinateSystem, startPosition, endPosition);
        return new RepetitiveRegion(location, repeatFamily);
    }

    private RepetitiveRegion(GenomicRegion location, RepeatFamily repeatFamily) {
        super(location);
        this.repeatFamily = repeatFamily;
    }

    public RepeatFamily repeatFamily() {
        return repeatFamily;
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
                "repeatFamily=" + repeatFamily +
                "} " + super.toString();
    }
}
