package org.jax.svanna.model.landscape.dosage;

import org.jax.svanna.model.landscape.BaseLocated;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;
import java.util.Objects;

class DosageRegionDefault extends BaseLocated implements DosageRegion {

    private final List<Dosage> dosages;

    static DosageRegionDefault of(GenomicRegion location, List<Dosage> dosages) {
        return new DosageRegionDefault(location, dosages);
    }

    private DosageRegionDefault(GenomicRegion location, List<Dosage> dosages) {
        super(location);
        this.dosages = Objects.requireNonNull(dosages, "Dosage data must not be null");
    }

    @Override
    public List<Dosage> dosages() {
        return dosages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DosageRegionDefault that = (DosageRegionDefault) o;
        return Objects.equals(dosages, that.dosages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dosages);
    }

    @Override
    public String toString() {
        return "DosageRegion{" +
                "dosages=" + dosages +
                "} " + super.toString();
    }
}
