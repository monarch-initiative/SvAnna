package org.jax.svann.parse;

import org.jax.svann.TestBase;
import org.jax.svann.reference.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class MergedStructuralRearrangementParserTest extends TestBase {

    private static final Path SV_SMALL_MERGED = Paths.get("src/test/resources/sv_small_merged.bed");

    private MergedStructuralRearrangementParser parser;

    @BeforeEach
    public void setUp() {
        parser = new MergedStructuralRearrangementParser(GENOME_ASSEMBLY);
    }

    @Test
    public void parseFile() throws Exception {
        Collection<SequenceRearrangement> rearrangements = parser.parseFile(SV_SMALL_MERGED);

        assertThat(rearrangements, hasSize(3));

        Iterator<SequenceRearrangement> iterator = rearrangements.iterator();

        // deletion
        SequenceRearrangement deletion = iterator.next();
        assertThat(deletion.getType(), is(SvType.DELETION));
        assertThat(deletion.getAdjacencies(), hasSize(1));
        Adjacency adjacency = deletion.getAdjacencies().get(0);
        Breakend left = adjacency.getStart();
        assertThat(left.getPosition(), is(87615925));
        assertThat(left.getStrand(), is(Strand.FWD));

        Breakend right = adjacency.getEnd();
        assertThat(right.getPosition(), is(87616059));
        assertThat(right.getStrand(), is(Strand.FWD));

        // duplication
        SequenceRearrangement duplication = iterator.next();
        assertThat(duplication.getType(), is(SvType.DUPLICATION));
        assertThat(duplication.getAdjacencies(), hasSize(1));
        adjacency = duplication.getAdjacencies().get(0);
        left = adjacency.getStart();
        assertThat(left.getPosition(), is(89186234));
        assertThat(left.getStrand(), is(Strand.FWD));

        right = adjacency.getEnd();
        assertThat(right.getPosition(), is(89180725));
        assertThat(right.getStrand(), is(Strand.FWD));

        // inversion
        SequenceRearrangement inversion = iterator.next();
        assertThat(inversion.getType(), is(SvType.INVERSION));
        assertThat(inversion.getAdjacencies(), hasSize(2));
        Adjacency alpha = inversion.getAdjacencies().get(0);
        left = alpha.getStart();
        assertThat(left.getPosition(), is(95391950));
        assertThat(left.getStrand(), is(Strand.FWD));

        right = alpha.getEnd();
        assertThat(right.getPosition(), is(94821999));
        assertThat(right.getStrand(), is(Strand.REV));

        Adjacency beta = inversion.getAdjacencies().get(1);
        left = beta.getStart();
        assertThat(left.getPosition(), is(94822605));
        assertThat(left.getStrand(), is(Strand.REV));

        right = beta.getEnd();
        assertThat(right.getPosition(), is(95392558));
        assertThat(right.getStrand(), is(Strand.FWD));
    }
}