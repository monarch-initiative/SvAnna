package org.jax.svanna.core.reference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class EnhancerTest {

    private static final Contig ctg1 = Contig.of(1, "1", SequenceRole.ASSEMBLED_MOLECULE, "1", AssignedMoleculeType.CHROMOSOME, 10, "", "", "");

    private Enhancer enhancer;

    @BeforeEach
    public void setUp() {
        enhancer = Enhancer.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), Position.of(3), Position.of(6), .5, TermId.of("HP:0000001"), "brain");
    }

    @Test
    public void withStrand() {
        Enhancer ps = enhancer.withStrand(Strand.POSITIVE);

        assertThat(ps, is(sameInstance(enhancer)));

        Enhancer ns = enhancer.withStrand(Strand.NEGATIVE);
        assertThat(ns.contigName(), is("1"));
        assertThat(ns.start(), is(4));
        assertThat(ns.end(), is(7));
        assertThat(ns.strand(), is(Strand.NEGATIVE));
        assertThat(ns.tau(), is(.5));
        assertThat(ns.hpoId(), is(TermId.of("HP:0000001")));
        assertThat(ns.tissueLabel(), is("brain"));
    }

    @Test
    public void doubleWithStrand() {
        Enhancer region = enhancer.toOppositeStrand().toOppositeStrand();
        assertThat(region, equalTo(enhancer));
    }
}