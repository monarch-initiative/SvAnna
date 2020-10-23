package org.jax.svann.priority;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.structuralvar.SvType;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Set;

public interface SvPriority {


    SvImpact getImpact();

    Set<TranscriptModel> getAffectedTranscripts();

    Set<TermId> getAffectedGeneIds();

    SvType getSvType();
}
