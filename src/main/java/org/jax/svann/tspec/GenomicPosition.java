package org.jax.svann.tspec;

import java.util.Comparator;
import java.util.Objects;

import static java.util.Comparator.comparing;

public class GenomicPosition implements Comparable<GenomicPosition> {
    private final String chromosome;
    private final int position;
    /** false for negative strand. */
    private final boolean positiveStrand;

    public GenomicPosition(String chr, int pos, boolean ps) {
        this.chromosome = chr;
        this.position = pos;
        this.positiveStrand = ps;
    }

    public String getChromosome() {
        return chromosome;
    }

    public int getPosition() {
        return position;
    }

    public boolean isPositiveStrand() {
        return positiveStrand;
    }

    /** Be consistent with equals: use the same fields as getSigFields().*/
    private static Comparator<GenomicPosition> COMPARATOR =
            comparing(GenomicPosition::getChromosome)
                    .thenComparing(GenomicPosition::getPosition)
                    .thenComparing(GenomicPosition::isPositiveStrand);


    @Override
    public int compareTo(GenomicPosition that) {
        return COMPARATOR.compare(this, that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chromosome, position, positiveStrand);
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TssPosition)) {
            return false;
        }
        GenomicPosition that = (GenomicPosition) obj;
        return this.chromosome.equals(that.chromosome) &&
                this.position == that.position &&
                this.positiveStrand == that.positiveStrand;
    }
}
