package org.jax.svanna.core.hpo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class HpoDiseaseSummaryTest {

    @ParameterizedTest
    @CsvSource({
            "AUTOSOMAL_DOMINANT, X_DOMINANT",
            "AUTOSOMAL_RECESSIVE, X_RECESSIVE",
            "MITOCHONDRIAL, UNKNOWN",
    })
    public void compatibleWithInheritance(ModeOfInheritance first, ModeOfInheritance second) {
        Set<ModeOfInheritance> modes = Set.of(first, second);
        HpoDiseaseSummary disease = HpoDiseaseSummary.of("OMIM:123456", "Some unspecified disease", modes);
        for (ModeOfInheritance value : modes) {
            assertThat(disease.isCompatibleWithInheritance(value), equalTo(true));
        }

        Set<ModeOfInheritance> complement = Arrays.stream(ModeOfInheritance.values())
                .filter(moi -> !modes.contains(moi))
                .collect(Collectors.toSet());
        for (ModeOfInheritance value : complement) {
            assertThat(disease.isCompatibleWithInheritance(value), equalTo(false));
        }
    }

    @Test
    public void compatibleWithNoModeIfMissing() {
        HpoDiseaseSummary noModeOfInheritance = HpoDiseaseSummary.of("OMIM:123456", "Something", Set.of());
        for (ModeOfInheritance moi : ModeOfInheritance.values()) {
            assertThat(noModeOfInheritance.isCompatibleWithInheritance(moi), equalTo(false));
        }
    }
}