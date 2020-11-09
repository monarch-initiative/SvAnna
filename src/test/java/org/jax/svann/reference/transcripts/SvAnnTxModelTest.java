package org.jax.svann.reference.transcripts;

import org.jax.svann.TestBase;
import org.jax.svann.reference.GenomicRegion;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class SvAnnTxModelTest extends TestBase {

    private SvAnnTxModel transcript;

    private static final Contig CHR1 = GENOME_ASSEMBLY.getContigByName("chr1").orElseThrow();

    @BeforeEach
    public void setUp() {

        TxGenomicPosition txStart = new TxGenomicPosition(CHR1, 1_000, Strand.FWD);
        TxGenomicPosition txEnd = new TxGenomicPosition(CHR1, 2_000, Strand.FWD);


        TxGenomicPosition cdsStart = new TxGenomicPosition(CHR1, 1_200, Strand.FWD);
        TxGenomicPosition cdsEnd = new TxGenomicPosition(CHR1, 1_800, Strand.FWD);


        transcript = new SvAnnTxModel("acc", "GENE",
                txStart, txEnd,
                cdsStart, cdsEnd,
                List.of(
                        // exon 1
                        new TxGenomicRegion(
                                new TxGenomicPosition(CHR1, 1_000, Strand.FWD),
                                new TxGenomicPosition(CHR1, 1_300, Strand.FWD)),
                        // exon 2
                        new TxGenomicRegion(
                                new TxGenomicPosition(CHR1, 1_400, Strand.FWD),
                                new TxGenomicPosition(CHR1, 1_500, Strand.FWD)),
                        // exon 3
                        new TxGenomicRegion(
                                new TxGenomicPosition(CHR1, 1_700, Strand.FWD),
                                new TxGenomicPosition(CHR1, 2_000, Strand.FWD))
                ));
    }

    @Test
    public void withStrand() {
        SvAnnTxModel tx = transcript.withStrand(Strand.REV);

        assertThat(tx.getContig().getPrimaryName(), is("1"));
        assertThat(tx.getStrand(), is(Strand.REV));

        assertThat(tx.getStart().getPosition(), is(flipPosition(2_000)));
        assertThat(tx.getEnd().getPosition(), is(flipPosition(1_000)));

        List<GenomicRegion> exons = tx.getExons();
        assertThat(exons, hasSize(3));

        GenomicRegion first = exons.get(0);
        assertThat(first.getStartPosition(), is(flipPosition(2_000)));
        assertThat(first.getEndPosition(), is(flipPosition(1_700)));
        assertThat(first.getStrand(), is(Strand.REV));

        GenomicRegion second = exons.get(1);
        assertThat(second.getStartPosition(), is(flipPosition(1_500)));
        assertThat(second.getEndPosition(), is(flipPosition(1_400)));
        assertThat(second.getStrand(), is(Strand.REV));

        GenomicRegion third = exons.get(2);
        assertThat(third.getStartPosition(), is(flipPosition(1_300)));
        assertThat(third.getEndPosition(), is(flipPosition(1_000)));
        assertThat(third.getStrand(), is(Strand.REV));
    }

    private static int flipPosition(int pos) {
        return CHR1.getLength() - pos + 1;
    }
}