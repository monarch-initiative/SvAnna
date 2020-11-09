package org.jax.svann.reference.transcripts;

import org.jax.svann.reference.GenomicPosition;
import org.jax.svann.reference.GenomicRegion;
import org.jax.svann.reference.Strand;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The class holds the minimal data for a single transcript to be used within SvAnn.
 */
public class SvAnnTxModel implements GenomicRegion {

    private final String accessionId;

    private final String hgvsSymbol;

    private final GenomicPosition txStart;

    private final GenomicPosition txEnd;

    private final GenomicPosition cdsStart, cdsEnd;

    private final List<GenomicRegion> exons;

    SvAnnTxModel(String accessionId,
                 String hgvsSymbol,
                 GenomicPosition txStart,
                 GenomicPosition txEnd,
                 GenomicPosition cdsStart,
                 GenomicPosition cdsEnd,
                 List<GenomicRegion> exons) {
        this.accessionId = accessionId;
        this.hgvsSymbol = hgvsSymbol;
        this.txStart = txStart;
        this.txEnd = txEnd;
        this.cdsStart = cdsStart;
        this.cdsEnd = cdsEnd;
        this.exons = exons;

    }

    /**
     * @return position of the first base of the 5'UTR
     */
    @Override
    public GenomicPosition getStart() {
        return txStart;
    }

    /**
     * @return position of the last base of the 3'UTR
     */
    @Override
    public GenomicPosition getEnd() {
        return txEnd;
    }

    /**
     * Get transcript accession ID.
     *
     * @return string with accession ID, e.g. <code>NM_000123.4</code>, <code>ENST00000012345.6</code>
     */
    public String getAccession() {
        return accessionId;
    }

    /**
     * @return string with HGVS gene symbol
     */
    public String getGeneSymbol() {
        return hgvsSymbol;
    }

    /**
     * @return position of the first base of the first codon
     */
    public GenomicPosition getCdsStart() {
        return cdsStart;
    }

    /**
     * @return @return position of the last base of the termination codon
     */
    public GenomicPosition getCdsEnd() {
        return cdsEnd;
    }

    /**
     * @return list of regions corresponding to sequence that made it to the ribosome
     */
    public List<GenomicRegion> getExons() {
        return exons;
    }

    @Override
    public SvAnnTxModel withStrand(Strand strand) {
        if (getStrand().equals(strand)) {
            return this;
        } else {
            List<GenomicRegion> reversedExons = new ArrayList<>(exons.size());
            for (int i = exons.size() - 1; i >= 0; i--) {
                reversedExons.add(exons.get(i).withStrand(strand));
            }
            // cds & tx positions are provided in the switched order
            return new SvAnnTxModel(accessionId, hgvsSymbol,
                    txEnd.withStrand(strand),
                    txStart.withStrand(strand),
                    cdsEnd.withStrand(strand),
                    cdsStart.withStrand(strand),
                    reversedExons);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SvAnnTxModel that = (SvAnnTxModel) o;
        return Objects.equals(accessionId, that.accessionId) &&
                Objects.equals(hgvsSymbol, that.hgvsSymbol) &&
                Objects.equals(txStart, that.txStart) &&
                Objects.equals(txEnd, that.txEnd) &&
                Objects.equals(cdsStart, that.cdsStart) &&
                Objects.equals(cdsEnd, that.cdsEnd) &&
                Objects.equals(exons, that.exons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessionId, hgvsSymbol, txStart, txEnd, cdsStart, cdsEnd, exons);
    }

    @Override
    public String toString() {
        return hgvsSymbol + '(' + accessionId + ") "
                + getContig().getPrimaryName() + ":"
                + txStart.getPosition() + '-' + txEnd.getPosition()
                + '(' + getStrand() +')';
    }
}
