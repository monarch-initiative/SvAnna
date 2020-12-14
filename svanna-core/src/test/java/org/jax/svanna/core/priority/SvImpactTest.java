package org.jax.svanna.core.priority;

import org.jax.svanna.core.prioritizer.SvImpact;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SvImpactTest {

    @ParameterizedTest
    @CsvSource({
            "HIGH,HIGH,true",
            "HIGH,INTERMEDIATE,true",
            "HIGH,LOW,true",
            "INTERMEDIATE,HIGH,false",
            "INTERMEDIATE,INTERMEDIATE,true",
            "INTERMEDIATE,LOW,true",
            "LOW,HIGH,false",
            "LOW,INTERMEDIATE,false",
            "LOW,LOW,true",
            "UNKNOWN,HIGH,false",
            "UNKNOWN,INTERMEDIATE,false",
            "UNKNOWN,LOW,false"
    })
    public void satisfiesThreshold(SvImpact instance, SvImpact threshold, boolean expected) {
        assertThat(instance.satisfiesThreshold(threshold), is(expected));
    }
}