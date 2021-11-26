package org.jax.svanna.model.landscape.dosage;

import java.util.List;
import java.util.Objects;

/**
 * Dosage sensitivity data available for a single gene.
 */
public class GeneDosageData {

    private static final GeneDosageData EMPTY = new GeneDosageData(List.of());

    private final List<Dosage> dosages;

    public static GeneDosageData empty() {
        return EMPTY;
    }

    public static GeneDosageData of(List<Dosage> dosages) {
        Objects.requireNonNull(dosages, "Dosages must not be null");
        return dosages.isEmpty()
                ? EMPTY
                : new GeneDosageData(dosages);
    }

    private GeneDosageData(List<Dosage> dosages) {
        this.dosages = dosages;
    }

    public List<Dosage> dosages() {
        return dosages;
    }

    public boolean isHaploinsufficient() {
        return isHaploinsufficient(DosageSensitivityEvidence.LITTLE_EVIDENCE);
    }

    public boolean isHaploinsufficient(DosageSensitivityEvidence evidence) {
        return dosages.stream()
                .filter(d -> d.dosageSensitivityEvidence().isAtLeast(evidence))
                .anyMatch(d -> d.dosageSensitivity() == DosageSensitivity.HAPLOINSUFFICIENCY);
    }

    public boolean isTriplosensitive() {
        return isTriplosensitive(DosageSensitivityEvidence.LITTLE_EVIDENCE);
    }

    public boolean isTriplosensitive(DosageSensitivityEvidence evidence) {
        return dosages.stream()
                .filter(d -> d.dosageSensitivityEvidence().isAtLeast(evidence))
                .anyMatch(d -> d.dosageSensitivity() == DosageSensitivity.TRIPLOSENSITIVITY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeneDosageData that = (GeneDosageData) o;
        return Objects.equals(dosages, that.dosages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dosages);
    }

    @Override
    public String toString() {
        return "GeneDosageData{" +
                "dosages=" + dosages +
                '}';
    }
}
