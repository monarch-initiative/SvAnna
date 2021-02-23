package org.jax.svanna.core.priority.additive;

import org.monarchinitiative.svart.*;

import java.util.Objects;

class RouteLegDefault extends BaseGenomicRegion<RouteLegDefault> implements RouteLeg {

    private final String id;

    private final int contribution;

    static RouteLegDefault of(String id, Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, int contribution) {
        return new RouteLegDefault(id, contig, strand, coordinateSystem, startPosition, endPosition, contribution);
    }

    private RouteLegDefault(String id, Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, int contribution) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.id = id;
        this.contribution = contribution;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int contribution() {
        return contribution;
    }


    @Override
    protected RouteLegDefault newRegionInstance(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition) {
        return new RouteLegDefault(id, contig, strand, coordinateSystem, startPosition, endPosition, contribution);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RouteLegDefault that = (RouteLegDefault) o;
        return contribution == that.contribution && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, contribution);
    }

    @Override
    public String toString() {
        return "LegDefault{" +
                "id='" + id + '\'' +
                "contribution='" + contribution + '\'' +
                ", " + contigName() + ":" + start() + "-" + end() +
                '}';
    }
}
