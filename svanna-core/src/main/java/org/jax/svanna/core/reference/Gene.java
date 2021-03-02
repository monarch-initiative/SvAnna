package org.jax.svanna.core.reference;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.Set;

// Gene is a collection of transcripts with a unique accession ID and a name.
// Gene consists of >=1 transcript(s), all the transcripts must be on the same contig, strand, and coordinate system.
public interface Gene extends GenomicRegion {

    // TODO - evaluate usefulness of the `TermId`s
    TermId accessionId();

    TermId hgvsName();

    Set<Transcript> transcripts();

}
