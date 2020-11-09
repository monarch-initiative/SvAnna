package org.jax.svann.reference;

import org.jax.svann.reference.genome.Contig;

import java.util.Comparator;

/**
 * The class in line with <em>variant-api</em> GenomicPosition.
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public interface GenomicPosition extends Comparable<GenomicPosition> {

    Comparator<GenomicPosition> NATURAL_COMPARATOR = Comparator.comparing(GenomicPosition::getContigId)
            .thenComparing(GenomicPosition::getPosition)
            .thenComparing(GenomicPosition::getStrand)
            .thenComparing(GenomicPosition::getCi);

    Contig getContig();

    // Reserved range 1-25 in the case of human for the 'assembled-molecule' in the assembly report file
    default int getContigId() {
        return getContig().getId();
    }

    // column 0 of the assembly report file 1-22, X,Y,MT
    default String getContigName() {
        return getContig().getPrimaryName();
    }

    int getPosition();

    CoordinateSystem getCoordinateSystem();

    default Strand getStrand() {
        return Strand.FWD;
    }

    default ConfidenceInterval getCi() {
        return ConfidenceInterval.precise();
    }

//    public default int getMin() {
//        return getCi().getMinPos(getPosition());
//    }

//    public default int getMax() {
//        return getCi().getMaxPos(getPosition());
//    }

    GenomicPosition withStrand(Strand strand);

    default GenomicPosition toOppositeStrand() {
        return withStrand(getStrand().getOpposite());
    }

    @Override
    default int compareTo(GenomicPosition o) {
        return NATURAL_COMPARATOR.compare(this, o);
    }
}
