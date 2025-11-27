package org.monarchinitiative.svanna.ingest.parse;

import org.monarchinitiative.sgenes.gtf.io.GtfGeneParser;
import org.monarchinitiative.sgenes.gtf.io.GtfGeneParserFactory;
import org.monarchinitiative.sgenes.gtf.model.Biotype;
import org.monarchinitiative.sgenes.gtf.model.GencodeGene;
import org.monarchinitiative.sgenes.model.Gene;
import org.monarchinitiative.sgenes.model.GeneIdentifier;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.svart.assembly.SequenceRole;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GencodeGeneProcessor {

    private final Path localGencodeGtfPath;
    private final GenomicAssembly assembly;
    private final Map<String, String> hgncToNcbiGeneId;

    public GencodeGeneProcessor(
            Path localGencodeGtfPath,
            GenomicAssembly assembly,
            Map<String, String> hgncToNcbiGeneId) {
        this.localGencodeGtfPath = Objects.requireNonNull(localGencodeGtfPath);
        this.assembly = Objects.requireNonNull(assembly);
        this.hgncToNcbiGeneId = Objects.requireNonNull(hgncToNcbiGeneId);
    }

    public List<? extends Gene> process() {
        GtfGeneParser<GencodeGene> parser = GtfGeneParserFactory.gencodeGeneParser(localGencodeGtfPath, assembly);
        return parser.stream()
                .filter(geneIsCodingOrAtLeastOneTranscriptIsCoding())
                .flatMap(this::addNcbiGeneId)
                .collect(Collectors.toUnmodifiableList());
    }

    private Stream<Gene> addNcbiGeneId(GencodeGene g) {
        if (g.id().ncbiGeneId().isPresent()) {
            // We're done.
            // However, as of Nov 27th, 2025, Gencode genes do not come with an Entrez/NCBIGene id.
            return Stream.of(g);
        } else if (g.id().hgncId().isPresent()) {
            String hgncId = g.id().hgncId().get();
            String ncbiGeneId = hgncToNcbiGeneId.get(hgncId);
            if (ncbiGeneId != null) {
                // Patch the gene identifier with the NCBIGene id.
                GeneIdentifier gi = GeneIdentifier.of(g.id().accession(), g.id().symbol(), hgncId, ncbiGeneId);
                return Stream.of(
                        Gene.of(
                                gi,
                                g.location(),
                                g.transcriptStream().collect(Collectors.toList())
                        )
                );
            } else {
                return Stream.empty();
            }
        } else {
            return Stream.empty();
        }
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
