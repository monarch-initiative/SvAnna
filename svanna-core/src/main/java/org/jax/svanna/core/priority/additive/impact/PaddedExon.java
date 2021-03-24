package org.jax.svanna.core.priority.additive.impact;

import java.util.Objects;

class PaddedExon {

    private final int paddedStart;
    private final int start;
    private final int end;
    private final int paddedEnd;
    private final int nCodingBases;

    static PaddedExon of(int paddedStart, int start, int end, int paddedEnd, int nCodingBases) {
        return new PaddedExon(paddedStart, start, end, paddedEnd, nCodingBases);
    }

    private PaddedExon(int paddedStart, int start, int end, int paddedEnd, int nCodingBases) {
        this.paddedStart = paddedStart;
        this.start = start;
        this.end = end;
        this.paddedEnd = paddedEnd;
        this.nCodingBases = nCodingBases;
    }

    public int paddedStart() {
        return paddedStart;
    }

    public int start() {
        return start;
    }

    public int end() {
        return end;
    }

    public int paddedEnd() {
        return paddedEnd;
    }

    public int nCodingBases() {
        return nCodingBases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaddedExon that = (PaddedExon) o;
        return paddedStart == that.paddedStart && start == that.start && end == that.end && paddedEnd == that.paddedEnd;
    }

    @Override
    public int hashCode() {
        return Objects.hash(paddedStart, start, end, paddedEnd);
    }

    @Override
    public String toString() {
        return "PaddedExon{" +
                "paddedStart=" + paddedStart +
                ", start=" + start +
                ", end=" + end +
                ", paddedEnd=" + paddedEnd +
                '}';
    }
}
