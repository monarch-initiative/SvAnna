package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Position;
import org.monarchinitiative.svart.Region;

import java.util.Objects;

public class Exon implements Region<Exon> {

    private final Position startPosition, endPosition;
    private final CoordinateSystem coordinateSystem;

    public static Exon of(CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new Exon(coordinateSystem, startPosition, endPosition);
    }

    private Exon(CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.coordinateSystem = coordinateSystem;
    }


    @Override
    public Position startPosition() {
        return startPosition;
    }

    @Override
    public Position endPosition() {
        return endPosition;
    }

    @Override
    public CoordinateSystem coordinateSystem() {
        return coordinateSystem;
    }

    @Override
    public Exon withCoordinateSystem(CoordinateSystem coordinateSystem) {
        return new Exon(coordinateSystem, startPositionWithCoordinateSystem(coordinateSystem), endPositionWithCoordinateSystem(coordinateSystem));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exon exon = (Exon) o;
        return Objects.equals(startPosition, exon.startPosition) && Objects.equals(endPosition, exon.endPosition) && coordinateSystem == exon.coordinateSystem;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPosition, endPosition, coordinateSystem);
    }

    @Override
    public String toString() {
        return "Exon{" +
                "startPosition=" + startPosition +
                ", endPosition=" + endPosition +
                ", coordinateSystem=" + coordinateSystem +
                '}';
    }
}
