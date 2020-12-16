package org.jax.svanna.core.filter;

import org.jax.svanna.core.TestDataConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestDataConfig.class)
public class StructuralVariantFrequencyFilterTest {

    private StructuralVariantFrequencyFilter filter;


    @Test
    public void name() {
        assertTrue(true);
        assertThat(true, equalTo(true));
    }
}