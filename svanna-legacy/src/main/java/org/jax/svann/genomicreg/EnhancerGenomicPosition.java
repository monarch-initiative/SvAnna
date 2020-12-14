package org.jax.svann.genomicreg;

import org.jax.svann.reference.CoordinateSystem;
import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Package private implementation of {@link GenomicPosition}.
 */
class EnhancerGenomicPosition implements GenomicPosition {
    private final Contig chromosome;
    private final int pos;
    private final Strand strand;

    EnhancerGenomicPosition(Contig chr, int pos, Strand strand, CoordinateSystem coordinateSystem) {
        this.chromosome = chr;
        this.pos = coordinateSystem.equals(CoordinateSystem.ZERO_BASED) ? pos + 1 : pos;
        this.strand = strand;
    }

    public Contig getContig() {
        return chromosome;
    }

    public int getPosition() {
        return pos;
    }

    @Override
    public CoordinateSystem getCoordinateSystem() {
        return CoordinateSystem.ONE_BASED;
    }

    @Override
    public EnhancerGenomicPosition withStrand(Strand strand) {
        if (this.strand.equals(strand)) {
            return this;
        } else {
            int position = getContig().getLength() - pos + 1;
            return new EnhancerGenomicPosition(chromosome, position, strand, CoordinateSystem.ONE_BASED);
        }
    }

    public boolean isPositiveStrand() {
        return strand.isForward();
    }

    /**
     * Be consistent with equals: use the same fields as getSigFields().
     */
//    private static final Comparator<GenomicPosition> COMPARATOR =
//            comparing(GenomicPosition::getContig)
//                    .thenComparing(GenomicPosition::getPosition)
//                    .thenComparing(GenomicPosition::isPositiveStrand);
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnhancerGenomicPosition that = (EnhancerGenomicPosition) o;
        return pos == that.pos &&
                Objects.equals(chromosome, that.chromosome) &&
                strand == that.strand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chromosome, pos, strand);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EnhancerGenomicPosition.class.getSimpleName() + "[", "]")
                .add("chromosome=" + chromosome)
                .add("pos=" + pos)
                .add("strand=" + strand)
                .toString();
    }
}
