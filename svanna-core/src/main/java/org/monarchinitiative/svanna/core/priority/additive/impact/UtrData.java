package org.monarchinitiative.svanna.core.priority.additive.impact;

import java.util.Objects;

/**
 * Container for UTR-related coordinates for a coding transcript.
 * <p>
 * NOTE - USING THE SAME COORDINATE SYSTEM FOR THE COORDINATES IS OF PARAMOUNT IMPORTANCE.
 */
class UtrData {

    private final int txStart;
    private final int cdsStart;
    private final int cdsEnd;
    private final int txEnd;

    // MAKE SURE THESE ARE IN THE SAME COORDINATE SYSTEM!
    static UtrData of(int txStart, int cdsStart, int cdsEnd, int txEnd) {
        return new UtrData(txStart, cdsStart, cdsEnd, txEnd);
    }

    private UtrData(int txStart, int cdsStart, int cdsEnd, int txEnd) {
        this.txStart = txStart;
        this.cdsStart = cdsStart;
        this.cdsEnd = cdsEnd;
        this.txEnd = txEnd;
    }

    public int txStart() {
        return txStart;
    }

    public int cdsStart() {
        return cdsStart;
    }

    public int cdsEnd() {
        return cdsEnd;
    }

    public int txEnd() {
        return txEnd;
    }

    public int fiveUtrLength() {
        return cdsStart - txStart;
    }

    public int threeUtrLength() {
        return txEnd - cdsEnd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtrData utrData = (UtrData) o;
        return txStart == utrData.txStart && cdsStart == utrData.cdsStart && cdsEnd == utrData.cdsEnd && txEnd == utrData.txEnd;
    }

    @Override
    public int hashCode() {
        return Objects.hash(txStart, cdsStart, cdsEnd, txEnd);
    }

    @Override
    public String toString() {
        return "UtrData{" +
                "txStart=" + txStart +
                ", cdsStart=" + cdsStart +
                ", cdsEnd=" + cdsEnd +
                ", txEnd=" + txEnd +
                '}';
    }
}
