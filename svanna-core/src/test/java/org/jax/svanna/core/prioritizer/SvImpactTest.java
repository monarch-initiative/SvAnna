package org.jax.svanna.core.prioritizer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class SvImpactTest {

    @ParameterizedTest
    @CsvSource({
            "VERY_HIGH,     VERY_HIGH,    true",
            "VERY_HIGH,     HIGH,         true",
            "VERY_HIGH,     INTERMEDIATE, true",
            "VERY_HIGH,     LOW,          true",

            "HIGH,          VERY_HIGH,    false",
            "HIGH,          HIGH,         true",
            "HIGH,          INTERMEDIATE, true",
            "HIGH,          LOW,          true",

            "INTERMEDIATE,  VERY_HIGH,    false",
            "INTERMEDIATE,  HIGH,         false",
            "INTERMEDIATE,  INTERMEDIATE, true",
            "INTERMEDIATE,  LOW,          true",

            "LOW,           VERY_HIGH,    false",
            "LOW,           HIGH,         false",
            "LOW,           INTERMEDIATE, false",
            "LOW,           LOW,          true",

            "UNKNOWN,       VERY_HIGH,    false",
            "UNKNOWN,       HIGH,         false",
            "UNKNOWN,       INTERMEDIATE, false",
            "UNKNOWN,       LOW,          false"
    })
    public void satisfiesThreshold(SvImpact instance, SvImpact threshold, boolean expected) {
        assertThat(instance.satisfiesThreshold(threshold), is(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "VERY_HIGH,     VERY_HIGH",
            "HIGH,          VERY_HIGH",
            "INTERMEDIATE,  HIGH",
            "LOW,           INTERMEDIATE",
            "UNKNOWN,       UNKNOWN",

    })
    public void incrementSeverity(SvImpact instance, SvImpact expected) {
        assertThat(instance.incrementSeverity(), equalTo(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "VERY_HIGH,     HIGH",
            "HIGH,          INTERMEDIATE",
            "INTERMEDIATE,  LOW",
            "LOW,           LOW",
            "UNKNOWN,       UNKNOWN",
    })
    public void decrementSeverity(SvImpact instance, SvImpact expected) {
        assertThat(instance.decrementSeverity(), equalTo(expected));
    }

}