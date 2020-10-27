package org.jax.svann.parse;

import org.jax.svann.TestBase;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class BreakendAssemblerTest extends TestBase {

    private static final Charset CHARSET = StandardCharsets.US_ASCII;
    private static Contig CHR2, CHR13, CHR17;
    private BreakendAssembler assembler;

    @BeforeAll
    public static void beforeAll() throws Exception {
        CHR2 = GENOME_ASSEMBLY.getContigByName("chr2").get();
        CHR13 = GENOME_ASSEMBLY.getContigByName("chr2").get();
        CHR17 = GENOME_ASSEMBLY.getContigByName("chr17").get();
    }

    @BeforeEach
    public void setUp() {
        assembler = new BreakendAssembler();
    }

    @Test
    public void assemble() {
        final BreakendRecord bnd_W = new BreakendRecord(ChromosomalPosition.of(CHR2, Position.precise(321681), Strand.FWD),
                "bnd_W", "bnd_Y", "G", "G]17:198982]");
        final BreakendRecord bnd_Y = new BreakendRecord(ChromosomalPosition.of(CHR17, Position.precise(198982), Strand.FWD),
                "bnd_Y", "bnd_W", "A", "A]2:321681]");

        final BreakendRecord bnd_V = new BreakendRecord(ChromosomalPosition.of(CHR2, Position.precise(321682), Strand.FWD),
                "bnd_V", "bnd_U", "T", "]13:123456]T");
        final BreakendRecord bnd_U = new BreakendRecord(ChromosomalPosition.of(CHR13, Position.precise(123456), Strand.FWD),
                "bnd_U", "bnd_V", "C", "C[2:321682[");

        final BreakendRecord bnd_X = new BreakendRecord(ChromosomalPosition.of(CHR13, Position.precise(198982), Strand.FWD),
                "bnd_X", "bnd_Z", "A", "[17:198983[A");
        final BreakendRecord bnd_Z = new BreakendRecord(ChromosomalPosition.of(CHR17, Position.precise(198983), Strand.FWD),
                "bnd_Z", "bnd_X", "C", "[13:123457[C");

        final List<SequenceRearrangement> rearrangements = assembler.assemble(Set.of(bnd_W, bnd_Y, bnd_V, bnd_U, bnd_X, bnd_Z));
        rearrangements.forEach(System.err::println);
    }
}