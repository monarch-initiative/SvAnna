package org.jax.svanna.model.landscape.dosage;

import org.jax.svanna.model.landscape.BaseLocated;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.Objects;

public class DosageRegion extends BaseLocated {

    private final GeneDosageData geneDosageData;

    public static DosageRegion of(GenomicRegion location, GeneDosageData geneDosageData) {
        return new DosageRegion(location, geneDosageData);
    }

    private DosageRegion(GenomicRegion location, GeneDosageData geneDosageData) {
        super(location);
        this.geneDosageData = geneDosageData;
    }

    public GeneDosageData geneDosageData() {
        return geneDosageData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DosageRegion that = (DosageRegion) o;
        return Objects.equals(geneDosageData, that.geneDosageData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), geneDosageData);
    }

    @Override
    public String toString() {
        return "DosageRegion{" +
                "geneDosageData=" + geneDosageData +
                "} " + super.toString();
    }
}
