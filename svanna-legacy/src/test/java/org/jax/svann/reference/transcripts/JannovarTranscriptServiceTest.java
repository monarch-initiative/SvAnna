package org.jax.svann.reference.transcripts;

import org.jax.svann.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JannovarTranscriptServiceTest extends TestBase {

    private JannovarTranscriptService service;

    @BeforeEach
    public void setUp() {
        service = JannovarTranscriptService.of(GENOME_ASSEMBLY, JANNOVAR_DATA);
    }

    @Test
    public void getChromosomeMap() {
        // let's do some superficial tests for now
        assertThat(service.getChromosomeMap().keySet(), hasItems(1, 7, 9, 13, 15, 20, 23, 24));
    }

    @ParameterizedTest
    @CsvSource({
            "1,15",
            "7,8",
            "9,5",
            "13,1",
            "15,1",
            "20,11",
            "23,8",
            "24,1"})
    public void checkSizes(int contig, int size) {
        // let's do some superficial tests for now
        assertThat(service.getChromosomeMap().get(contig).size(), is(size));
    }
}