package org.jax.svann.reference.transcripts;

import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.GenomicRegion;
import org.jax.svann.reference.Strand;

import java.util.Objects;

/**
 * Yet another implementation of the genomic region.
 */
class TxGenomicRegion implements GenomicRegion {

    private final GenomicPosition start, end;

    TxGenomicRegion(GenomicPosition start, GenomicPosition end) {
        this.start = start;
        this.end = end;
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
    public TxGenomicRegion withStrand(Strand strand) {
        if (getStrand().equals(strand)) {
            return this;
        } else {
            return new TxGenomicRegion(end.withStrand(strand), start.withStrand(strand));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TxGenomicRegion that = (TxGenomicRegion) o;
        return Objects.equals(start, that.start) &&
                Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "TxGenomicRegion{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
