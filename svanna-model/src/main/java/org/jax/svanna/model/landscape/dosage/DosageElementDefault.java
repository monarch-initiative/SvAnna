package org.jax.svanna.model.landscape.dosage;

import org.jax.svanna.model.landscape.BaseLocated;
import org.monarchinitiative.svart.*;

import java.util.Objects;

class DosageElementDefault extends BaseLocated implements DosageElement {

    private final String id;
    private final DosageSensitivity dosageSensitivity;
    private final DosageSensitivityEvidence evidence;

    DosageElementDefault(GenomicRegion location,
                         String id,
                         DosageSensitivity dosageSensitivity,
                         DosageSensitivityEvidence evidence) {
        super(location);
        this.id = id;
        this.dosageSensitivity = dosageSensitivity;
        this.evidence = evidence;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public DosageSensitivity dosageSensitivity() {
        return dosageSensitivity;
    }

    @Override
    public DosageSensitivityEvidence dosageSensitivityEvidence() {
        return evidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DosageElementDefault that = (DosageElementDefault) o;
        return Objects.equals(id, that.id) && dosageSensitivity == that.dosageSensitivity && evidence == that.evidence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, dosageSensitivity, evidence);
    }

    @Override
    public String toString() {
        return "DosageElementDefault{" +
                "id='" + id + '\'' +
                ", dosageSensitivity=" + dosageSensitivity +
                ", evidence=" + evidence +
                "} " + super.toString();
    }
}
