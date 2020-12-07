package org.jax.svanna.core.reference;

import org.monarchinitiative.variant.api.*;

import java.util.Objects;

/**
 * This class implements the tasks that {@link GenomicRegion} needs to do.
 * TODO - replace/use by base genomic region once present in variant-api
 */
public class PreciseGenomicRegion implements GenomicRegion {

    protected final Contig contig;
    protected final Strand strand;
    protected final CoordinateSystem coordinateSystem;

    protected final Position start;
    protected final Position end;

    protected PreciseGenomicRegion(Contig contig,
                                   Strand strand,
                                   CoordinateSystem coordinateSystem,
                                   Position startPosition,
                                   Position endPosition) {
        this.contig = Objects.requireNonNull(contig, "Contig must not be null");
        this.strand = Objects.requireNonNull(strand, "Strand must not be null");
        this.coordinateSystem = Objects.requireNonNull(coordinateSystem, "Coordinate system must not be null");

        this.start = Objects.requireNonNull(startPosition, "Start must not be null");
        this.end = Objects.requireNonNull(endPosition, "End must not be null");

        if (coordinateSystem.isOneBased()) {
            if (startPosition.pos() > endPosition.pos()) {
                throw new IllegalArgumentException("Start position must not be higher than end position");
            }
        } else if (coordinateSystem.isZeroBased()) {
            if (startPosition.pos() >= endPosition.pos()) {
                throw new IllegalArgumentException("Start position must not be higher or equal to end position");
            }
        }
    }

    protected PreciseGenomicRegion(GenomicRegion region) {
        this(region.contig(), region.strand(), region.coordinateSystem(), region.startPosition(), region.endPosition());
    }

    protected static PreciseGenomicRegion zeroBased(Contig contig,
                                                    Strand strand,
                                                    int start,
                                                    int end) {
        return of(contig, strand, CoordinateSystem.ZERO_BASED, Position.of(start), Position.of(end));
    }

    protected static PreciseGenomicRegion oneBased(Contig contig,
                                                   Strand strand,
                                                   int start,
                                                   int end) {
        return of(contig, strand, CoordinateSystem.ONE_BASED, Position.of(start), Position.of(end));
    }

    public static PreciseGenomicRegion of(Contig contig,
                                          Strand strand,
                                          CoordinateSystem coordinateSystem,
                                          Position startPosition,
                                          Position endPosition) {
        return new PreciseGenomicRegion(contig, strand, coordinateSystem, startPosition, endPosition);
    }

    @Override
    public Contig contig() {
        return contig;
    }

    @Override
    public Position startPosition() {
        return start;
    }

    @Override
    public Position endPosition() {
        return end;
    }

    @Override
    public Strand strand() {
        return strand;
    }

    @Override
    public GenomicRegion withStrand(Strand other) {
        if (strand == other) {
            return this;
        }
        Position startPos = start.invert(contig, coordinateSystem);
        Position endPos = end.invert(contig, coordinateSystem);
        return new PreciseGenomicRegion(contig, other, coordinateSystem, endPos, startPos);
    }

    @Override
    public CoordinateSystem coordinateSystem() {
        return CoordinateSystem.ZERO_BASED;
    }

    @Override
    public GenomicRegion withCoordinateSystem(CoordinateSystem coordinateSystem) {
        if (this.coordinateSystem == coordinateSystem) {
            return this;
        }
        return new PreciseGenomicRegion(contig, strand, coordinateSystem, normalisedStartPosition(coordinateSystem), end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PreciseGenomicRegion that = (PreciseGenomicRegion) o;
        return Objects.equals(contig, that.contig) &&
                Objects.equals(start, that.start) &&
                Objects.equals(end, that.end) &&
                strand == that.strand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(contig, start, end, strand);
    }
}
