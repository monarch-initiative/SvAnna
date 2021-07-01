package org.jax.svanna.core.reference;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Gene is a collection of transcripts with a unique accession ID and a name.
// Gene consists of >=1 transcript(s), all the transcripts must be on the same contig, strand, and coordinate system.
public interface Gene extends GenomicRegion {

    // TODO - evaluate usefulness of the `TermId`s
    // Should be e.g. `NCBIGene:5163` for PDK1
    TermId accessionId();

    String geneSymbol();

    Set<Transcript> nonCodingTranscripts();

    Set<CodingTranscript> codingTranscripts();

    default Set<Transcript> transcripts() {
        return Stream.concat(codingTranscripts().stream(), nonCodingTranscripts().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

}
