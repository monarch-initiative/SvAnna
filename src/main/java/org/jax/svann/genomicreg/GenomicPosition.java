package org.jax.svann.genomicreg;

import org.jax.svann.reference.Position;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

import java.util.Comparator;
import java.util.Objects;

import static java.util.Comparator.comparing;

/**
 * replace by ChromosomalPosition
 */
public class GenomicPosition implements Comparable<GenomicPosition> {
    private final Contig chromosome;
    private final Position position;
    private final Strand strand;

    public GenomicPosition(Contig chr, Position pos, Strand strand) {
        this.chromosome = chr;
        this.position = pos;
        this.strand = strand;
    }

    public Contig getChromosome() {
        return chromosome;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isPositiveStrand() {
        return this.strand.equals(Strand.FWD);
    }

    /** Be consistent with equals: use the same fields as getSigFields().*/
    private static final Comparator<GenomicPosition> COMPARATOR =
            comparing(GenomicPosition::getChromosome)
                    .thenComparing(GenomicPosition::getPosition)
                    .thenComparing(GenomicPosition::isPositiveStrand);


    @Override
    public int compareTo(GenomicPosition that) {
        return COMPARATOR.compare(this, that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chromosome, position, strand);
    }

    @Override
    public boolean equals(Object obj) {
        Objects.requireNonNull(obj);
        if (! (obj instanceof TssPosition)) {
            return false;
        }
        GenomicPosition that = (GenomicPosition) obj;
        return this.chromosome.equals(that.chromosome) &&
                this.position == that.position &&
                this.strand == that.strand;
    }
}
