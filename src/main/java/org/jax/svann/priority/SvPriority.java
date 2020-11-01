package org.jax.svann.priority;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.SvType;

import java.util.List;
import java.util.Set;

public interface SvPriority {

    SequenceRearrangement getRearrangement();

    SvType getType();
    SvImpact getImpact();

    List<HpoDiseaseSummary> getDiseases();

    Set<TranscriptModel> getAffectedTranscripts();

    Set<GeneWithId> getAffectedGeneIds();

    List<Enhancer> getAffectedEnhancers();

    /** If true, the SV overlaps with a transcript or genomic regulatory element that is annotated
     * to an HPO term representing the phenotypic observations in the proband.
     * @return true if the SV disrupts a gene or enhancer of potential phenotypic relevance.
     */
    boolean hasPhenotypicRelevance();
}
