package org.jax.svann.reference;

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

    default int getEndPosition() {
        return getEnd().getPosition();
    }

    default ConfidenceInterval getEndCi() {
        return getEnd().getCi();
    }

    default int getLength() {
        return getEnd().getPosition() - getStart().getPosition();
    }
}
