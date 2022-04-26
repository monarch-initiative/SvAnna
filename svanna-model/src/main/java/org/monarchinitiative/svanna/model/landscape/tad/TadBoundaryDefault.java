package org.monarchinitiative.svanna.model.landscape.tad;

import org.monarchinitiative.svanna.model.landscape.BaseLocated;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.Objects;

// TODO - this should not be public
public class TadBoundaryDefault extends BaseLocated implements TadBoundary {

    private final String id;

    private final float stability;

    private TadBoundaryDefault(GenomicRegion location, String id, float stability) {
        super(location);
        this.id = id;
        this.stability = stability;
    }

    public static TadBoundaryDefault of(GenomicRegion location, String id, float stability) {
        return new TadBoundaryDefault(location, id, stability);
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
                "id='" + id + '\'' +
                ", stability=" + stability +
                "} " + super.toString();
    }
}
