package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.*;

import java.util.List;
import java.util.Objects;

abstract class BaseTranscript<T extends BaseTranscript<?>> extends BaseGenomicRegion<T> implements Transcript {

    private final String accessionId;
    private final List<Exon> exons;

    protected BaseTranscript(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition, String accessionId, List<Exon> exons) {
        super(contig, strand, coordinateSystem, startPosition, endPosition);
        this.accessionId = accessionId;
        this.exons = exons;
    }


    @Override
    public String accessionId() {
        return accessionId;
    }

    @Override
    public List<Exon> exons() {
        return exons;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        BaseTranscript<?> that = (BaseTranscript<?>) o;
        return Objects.equals(accessionId, that.accessionId) && Objects.equals(exons, that.exons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessionId, exons);
    }
}
