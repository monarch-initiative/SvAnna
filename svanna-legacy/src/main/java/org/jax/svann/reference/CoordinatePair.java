package org.jax.svann.reference;

/**
 * This interface represents an ordered pair of coordinates, suitable to be used for representing both
 * <em>intra</em>chromosomal events (e.g inversions, deletions), and <em>inter</em>chromosomal events (translocations).
 * <p>
 * Note that multiple coordinate pairs might be used to represent an event.
 */
public interface CoordinatePair {

    GenomicPosition getStart();

    // Reserved range 1-25 in the case of human for the 'assembled-molecule' in the assembly report file
    default int getStartContigId() {
        return getStart().getContig().getId();
    }

    // column 0 of the assembly report file 1-22, X,Y,MT
    default String getStartContigName() {
        return getStart().getContig().getPrimaryName();
    }

    /**
     * @return one-based (inclusive) start coordinate
     */
    default int getStartPosition() {
        return getStart().getPosition();
    }

    default ConfidenceInterval getStartCi() {
        return getStart().getCi();
    }

    GenomicPosition getEnd();

    // Reserved range 1-25 in the case of human for the 'assembled-molecule' in the assembly report file
    default int getEndContigId() {
        return getEnd().getContig().getId();
    }

    // column 0 of the assembly report file 1-22, X,Y,MT
    default String getEndContigName() {
        return getEnd().getContig().getPrimaryName();
    }

    /**
     * @return one-based (inclusive) end coordinate
     */
    default int getEndPosition() {
        return getEnd().getPosition();
    }

    default ConfidenceInterval getEndCi() {
        return getEnd().getCi();
    }

    default int getLength() {
        return getEnd().getPosition() - getStart().getPosition();
    }

    /**
     * @return <code>true</code> if both coordinates are located on a single contig
     */
    default boolean isIntrachromosomal() {
        return getStartContigId() == getEndContigId();
    }

    /**
     * @return <code>true</code> if the coordinates are located on two distinct contigs
     */
    default boolean isInterchromosomal() {
        return getStartContigId() != getEndContigId();
    }
}
