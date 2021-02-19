package org.jax.svanna.core.prioritizer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SvImpactTest {

    @ParameterizedTest
    @CsvSource({
            "VERY_HIGH,     VERY_HIGH,    true",
            "VERY_HIGH,     HIGH,         true",
            "VERY_HIGH,     INTERMEDIATE, true",
            "VERY_HIGH,     LOW,          true",
            "VERY_HIGH,     VERY_LOW,     true",

            "HIGH,          VERY_HIGH,    false",
            "HIGH,          HIGH,         true",
            "HIGH,          INTERMEDIATE, true",
            "HIGH,          LOW,          true",
            "HIGH,          VERY_LOW,     true",

            "INTERMEDIATE,  VERY_HIGH,    false",
            "INTERMEDIATE,  HIGH,         false",
            "INTERMEDIATE,  INTERMEDIATE, true",
            "INTERMEDIATE,  LOW,          true",
            "INTERMEDIATE,  VERY_LOW,     true",

            "LOW,           VERY_HIGH,    false",
            "LOW,           HIGH,         false",
            "LOW,           INTERMEDIATE, false",
            "LOW,           LOW,          true",
            "LOW,           VERY_LOW,     true",

            "VERY_LOW,      VERY_HIGH,    false",
            "VERY_LOW,      HIGH,         false",
            "VERY_LOW,      INTERMEDIATE, false",
            "VERY_LOW,      LOW,          false",
            "VERY_LOW,      VERY_LOW,     true",

            "UNKNOWN,       VERY_HIGH,    false",
            "UNKNOWN,       HIGH,         false",
            "UNKNOWN,       INTERMEDIATE, false",
            "UNKNOWN,       LOW,          false",
            "UNKNOWN,       VERY_LOW,     false"
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
            "VERY_LOW,      LOW",
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
            "LOW,           VERY_LOW",
            "VERY_LOW,      VERY_LOW",
            "UNKNOWN,       UNKNOWN",
    })
    public void decrementSeverity(SvImpact instance, SvImpact expected) {
        assertThat(instance.decrementSeverity(), equalTo(expected));
    }

    @ParameterizedTest
    @CsvSource({
            "VERY_HIGH,     1.0",
            "HIGH,           .8",
            "INTERMEDIATE,   .6",
            "LOW,            .4",
            "VERY_LOW,       .2",
            "UNKNOWN,        .0",

    })
    public void priority(SvImpact instance, double expected) {
        assertThat(instance.priority(), closeTo(expected, 1E-12));
    }

}