package org.jax.svanna.core.hpo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class offers a simple POJO with information about HpoDiseases that are required for output in the
 * list of prioritized structural variants.
 *
 * @author Daniel Danis
 * @author Peter N Robinson
 */
public class HpoDiseaseSummary {

    private static final Map<ModeOfInheritance, Integer> COMPATIBILITY_MAP = Map.of(
            ModeOfInheritance.AUTOSOMAL_DOMINANT, 0x01,  // hex for 0000 0001
            ModeOfInheritance.AUTOSOMAL_RECESSIVE, 0x02, // hex for 0000 0010
            ModeOfInheritance.X_DOMINANT, 0x4,           // hex for 0000 0100
            ModeOfInheritance.X_RECESSIVE, 0x8,          // hex for 0000 1000
            ModeOfInheritance.MITOCHONDRIAL, 0x10,       // hex for 0001 0000
            ModeOfInheritance.Y_LINKED, 0x20,            // hex for 0010 0000
            ModeOfInheritance.UNKNOWN, 0x40              // hex for 0100 0000
    );

    private final String diseaseId;
    private final String diseaseName;
    private final int inheritanceModeCompatibility;

    public static HpoDiseaseSummary of(String diseaseId, String diseaseName, Set<ModeOfInheritance> inheritanceModeCompatibility) {
        int compatibility = inheritanceModeCompatibility.stream().mapToInt(moi -> COMPATIBILITY_MAP.getOrDefault(moi, 0x0)).sum();
        return new HpoDiseaseSummary(diseaseId, diseaseName, compatibility);
    }

    private HpoDiseaseSummary(String diseaseId, String diseaseName, int inheritanceModeCompatibility) {
        this.diseaseId = Objects.requireNonNull(diseaseId);
        this.diseaseName = Objects.requireNonNull(diseaseName);
        this.inheritanceModeCompatibility = inheritanceModeCompatibility;
    }

    public String getDiseaseId() {
        return diseaseId;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    /**
     * @return <code>true</code> if segregation of the disease has been observed with given {@code modeOfInheritance}
     */
    public boolean isCompatibleWithInheritance(ModeOfInheritance modeOfInheritance) {
        return (COMPATIBILITY_MAP.getOrDefault(modeOfInheritance, 0x0) & inheritanceModeCompatibility) >= 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HpoDiseaseSummary that = (HpoDiseaseSummary) o;
        return Objects.equals(diseaseId, that.diseaseId) && Objects.equals(diseaseName, that.diseaseName) && inheritanceModeCompatibility == that.inheritanceModeCompatibility;
    }

    @Override
    public int hashCode() {
        return Objects.hash(diseaseId, diseaseName, inheritanceModeCompatibility);
    }

    @Override
    public String toString() {
        return "HpoDiseaseSummary{" +
                "diseaseId='" + diseaseId + '\'' +
                ", diseaseName='" + diseaseName + '\'' +
                '}';
    }
}
