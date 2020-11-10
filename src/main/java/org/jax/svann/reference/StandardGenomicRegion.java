package org.jax.svann.reference;

import org.jax.svann.reference.genome.Contig;

import java.util.Objects;

/**
 * The simplest implementation of the genomic region.
 */
public class StandardGenomicRegion implements GenomicRegion {

    private final GenomicPosition start, end;

    private StandardGenomicRegion(GenomicPosition start, GenomicPosition end) {
        this.start = Objects.requireNonNull(start);
        this.end = Objects.requireNonNull(end);

        if (start.getContigId() != end.getContigId()) {
            throw new ContigMismatchException("Start and end must be on the same contig: " + start.getContigName() + "!=" + end.getContigName());
        }
        if (!start.getStrand().equals(end.getStrand())) {
            throw new InvalidCoordinatesException("Start and end must be on the same strand");
        }


    }

    public static StandardGenomicRegion precise(Contig contig, int start, int end, Strand strand) {
        StandardGenomicPosition startPos = StandardGenomicPosition.precise(contig, start, strand);
        StandardGenomicPosition endPos = StandardGenomicPosition.precise(contig, end, strand);
        return new StandardGenomicRegion(startPos, endPos);
    }

    public static StandardGenomicRegion of(GenomicPosition start, GenomicPosition end) {
        return new StandardGenomicRegion(start, end);
    }

    @Override
    public GenomicPosition getStart() {
        return start;
    }

    @Override
    public GenomicPosition getEnd() {
        return end;
    }

    @Override
    public StandardGenomicRegion withStrand(Strand strand) {
        if (getStrand().equals(strand)) {
            return this;
        } else {
            return new StandardGenomicRegion(end.withStrand(strand), start.withStrand(strand));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardGenomicRegion that = (StandardGenomicRegion) o;
        return Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "GReg{" + start +
                " - " + end + '}';
    }
}
