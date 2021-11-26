package org.jax.svanna.model.landscape.dosage;

import java.util.Objects;

public class Dosage {

    private final String id;
    private final DosageSensitivity dosageSensitivity;
    private final DosageSensitivityEvidence dosageSensitivityEvidence;

    private Dosage(String id, DosageSensitivity dosageSensitivity, DosageSensitivityEvidence dosageSensitivityEvidence) {
        this.id = id;
        this.dosageSensitivity = dosageSensitivity;
        this.dosageSensitivityEvidence = dosageSensitivityEvidence;
    }

    public static Dosage of(String id,
                            DosageSensitivity dosageSensitivity,
                            DosageSensitivityEvidence dosageSensitivityEvidence) {
        return new Dosage(id, dosageSensitivity, dosageSensitivityEvidence);
    }

    public String id() {
        return id;
    }

    public DosageSensitivity dosageSensitivity() {
        return dosageSensitivity;
    }

    public DosageSensitivityEvidence dosageSensitivityEvidence() {
        return dosageSensitivityEvidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dosage that = (Dosage) o;
        return Objects.equals(id, that.id) && dosageSensitivity == that.dosageSensitivity && dosageSensitivityEvidence == that.dosageSensitivityEvidence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dosageSensitivity, dosageSensitivityEvidence);
    }

    @Override
    public String toString() {
        return "GeneDosage{" +
                "id='" + id + '\'' +
                ", dosageSensitivity=" + dosageSensitivity +
                ", dosageSensitivityEvidence=" + dosageSensitivityEvidence +
                '}';
    }
}
