package org.jax.svanna.ingest;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import xyz.ielis.silent.genes.gencode.io.GencodeParser;
import xyz.ielis.silent.genes.gencode.model.GencodeGene;
import xyz.ielis.silent.genes.io.GeneParser;
import xyz.ielis.silent.genes.io.GeneParserFactory;
import xyz.ielis.silent.genes.io.SerializationFormat;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prepare the file with genes used in `svanna-core/TestDataConfig`.
 */
@Disabled("To be run manually")
public class MakeSmallGencodeFileTest {

    private static final GenomicAssembly ASSEMBLY = GenomicAssemblies.GRCh38p13();

    @Test
    public void makeSmallGencodeFile() throws Exception {
        Path gencodeGtf = Path.of("/home/ielis/data/gencode/gencode.v39.basic.annotation.gtf.gz");
        Path output = Path.of("../svanna-core/src/test/resources/gencode.10genes.v38.basic.annotation.json.gz");

        // read Gencode genes & keep the target genes
        GencodeParser parser = new GencodeParser(gencodeGtf, ASSEMBLY);

        Set<String> targetGeneSymbols = Set.of("SURF1", "SURF2", "FBN1", "ZNF436", "ZBTB48", "HNF4A", "GCK", "BRCA2", "COL4A5", "SRY");

        List<GencodeGene> targetGenes = parser.stream()
                .filter(g -> targetGeneSymbols.contains(g.id().symbol()))
                .collect(Collectors.toList());

        // write the target genes into the output
        GeneParserFactory parserFactory = GeneParserFactory.of(ASSEMBLY);
        GeneParser printer = parserFactory.forFormat(SerializationFormat.JSON);
        try (OutputStream os = new BufferedOutputStream(new GzipCompressorOutputStream(Files.newOutputStream(output)))) {
            printer.write(targetGenes, os);
        }
    }
}
