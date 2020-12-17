package org.jax.svanna.core.reference;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.variant.api.Contig;
import org.monarchinitiative.variant.api.GenomicAssembly;
import org.monarchinitiative.variant.api.SequenceRole;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class GenomicAssemblyProviderTest {

    private static final Path ASSEMBLY_REPORT_PATH = Paths.get("src/test/resources/GCA_000001405.28_GRCh38.p13_assembly_report.txt");

    @Test
    public void fromAssemblyReport() throws Exception {
        GenomicAssembly assembly = GenomicAssemblyProvider.fromAssemblyReport(ASSEMBLY_REPORT_PATH);

        assertThat(assembly.name(), is("GRCh38.p13"));
        assertThat(assembly.organismName(), is("Homo sapiens (human)"));
        assertThat(assembly.taxId(), is("9606"));
        assertThat(assembly.submitter(), is("Genome Reference Consortium"));
        assertThat(assembly.date(), is("2019-02-28"));
        assertThat(assembly.genBankAccession(), is("GCA_000001405.28"));
        assertThat(assembly.refSeqAccession(), is("GCF_000001405.39"));

        SortedSet<Contig> contigs = assembly.contigs();
        assertThat(contigs, hasSize(640));

        // check we have all primary chromosomes & mitochondrial DNA as well
        List<String> autosomeIds = IntStream.rangeClosed(1, 22).boxed().map(String::valueOf).collect(Collectors.toList());
        List<String> sexAndMitochondrial = List.of("X", "Y", "MT");
        for (String contigName : Stream.concat(autosomeIds.stream(), sexAndMitochondrial.stream()).collect(Collectors.toList())) {
            assertThat(assembly.contigByName(contigName), is(notNullValue()));
        }


        Contig mitochondrial = assembly.contigByName("MT");
        assertThat(mitochondrial.id(), is(25));
        assertThat(mitochondrial.name(), is("MT"));
        assertThat(mitochondrial.length(), is(16569));
        assertThat(mitochondrial.sequenceRole(), is(SequenceRole.ASSEMBLED_MOLECULE));
        assertThat(mitochondrial.genBankAccession(), is("J01415.2"));
        assertThat(mitochondrial.refSeqAccession(), is("NC_012920.1"));
        assertThat(mitochondrial.ucscName(), is("chrM"));
    }
}