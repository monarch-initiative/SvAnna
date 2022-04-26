package org.monarchinitiative.svanna.ingest.parse;

import org.monarchinitiative.sgenes.gtf.io.GtfGeneParser;
import org.monarchinitiative.sgenes.gtf.io.GtfGeneParserFactory;
import org.monarchinitiative.sgenes.gtf.model.Biotype;
import org.monarchinitiative.sgenes.gtf.model.GencodeGene;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.svart.assembly.SequenceRole;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GencodeGeneProcessor {

    private final Path localGencodeGtfPath;
    private final GenomicAssembly assembly;

    public GencodeGeneProcessor(Path localGencodeGtfPath, GenomicAssembly assembly) {
        this.localGencodeGtfPath = localGencodeGtfPath;
        this.assembly = assembly;
    }

    public List<? extends GencodeGene> process() {
        GtfGeneParser<GencodeGene> parser = GtfGeneParserFactory.gtfGeneParser(localGencodeGtfPath, assembly);
        return parser.stream()
                .filter(geneHasHgncId())
                .filter(geneIsCodingOrAtLeastOneTranscriptIsCoding())
                .collect(Collectors.toUnmodifiableList());
    }

    private Predicate<? super GencodeGene> geneHasHgncId() {
        return gene -> gene.id().hgncId().isPresent();
    }

    private static Predicate<? super GencodeGene> geneIsCodingOrAtLeastOneTranscriptIsCoding() {
        // Gene is located on assembled molecule of the genomic assembly and gene is coding or at least one transcript is coding
        return gene -> gene.contig().sequenceRole() == SequenceRole.ASSEMBLED_MOLECULE
                && (gene.biotype() == Biotype.protein_coding || gene.transcriptStream().anyMatch(tx -> tx.biotype() == Biotype.protein_coding));
    }

}
