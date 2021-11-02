package org.jax.svanna.core.reference;

import org.monarchinitiative.svart.BaseGenomicRegion;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.Strand;

import java.util.List;
import java.util.Objects;

abstract class BaseTranscript<T extends BaseTranscript<?>> extends BaseGenomicRegion<T> implements Transcript {

    private final String accessionId;
    private final List<Coordinates> exons;

    protected BaseTranscript(Contig contig, Strand strand, Coordinates coordinates, String accessionId, List<Coordinates> exons) {
        super(contig, strand, coordinates);
        this.accessionId = accessionId;
        this.exons = exons;
    }


    @Override
    public String accessionId() {
        return accessionId;
    }

    @Override
    public List<Coordinates> exons() {
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
