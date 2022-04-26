package org.monarchinitiative.svanna.model.landscape.dosage;

import org.monarchinitiative.svanna.model.landscape.BaseLocated;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.Objects;

class DosageRegionDefault extends BaseLocated implements DosageRegion {

    private final Dosage dosage;

    static DosageRegionDefault of(GenomicRegion location, Dosage dosage) {
        return new DosageRegionDefault(location, dosage);
    }

    private DosageRegionDefault(GenomicRegion location, Dosage dosage) {
        super(location);
        this.dosage = Objects.requireNonNull(dosage, "Dosage data must not be null");
    }

    @Override
    public Dosage dosage() {
        return dosage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DosageRegionDefault that = (DosageRegionDefault) o;
        return Objects.equals(dosage, that.dosage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dosage);
    }

    @Override
    public String toString() {
        return "DosageRegion{" +
                "dosage=" + dosage +
                "} " + super.toString();
    }
}
