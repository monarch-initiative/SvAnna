package org.jax.svann.overlap;

import org.jax.svann.parse.SvEvent;
import org.jax.svann.reference.IntrachromosomalEvent;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class OverlapperTest extends TestBase {

    private static Contig CHR9;

    private Overlapper overlapper;

    @BeforeAll
    public static void beforeAll() {
        CHR9 = GENOME_ASSEMBLY.getContigByName("chr9").get();
    }

    @BeforeEach
    public void setUp() {
        overlapper = new Overlapper(JANNOVAR_DATA);
    }

    @Test
    public void getOverlapList() {
        // TODO: 26. 10. 2020 implement
        IntrachromosomalEvent event = SvEvent.precise(CHR9, 136_224_000, 136_225_000, Strand.FWD, SvType.DELETION);
        final List<Overlap> overlaps = overlapper.getOverlapList(event);
        overlaps.forEach(System.err::println);
    }
}