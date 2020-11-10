package org.jax.svann.priority;

import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.reference.transcripts.SvAnnTxModel;

import java.util.List;
import java.util.Set;

public interface SvPriority {

    static SvPriority unknown() {
        return DefaultSvPriority.unknown();
    }

    SvImpact getImpact();

    List<HpoDiseaseSummary> getDiseases();

    Set<SvAnnTxModel> getAffectedTranscripts();

    Set<GeneWithId> getAffectedGeneIds();

    List<Enhancer> getAffectedEnhancers();

    List<Overlap> getOverlaps();

    /**
     * If true, the SV overlaps with a transcript or genomic regulatory element that is annotated
     * to an HPO term representing the phenotypic observations in the proband.
     *
     * @return true if the SV disrupts a gene or enhancer of potential phenotypic relevance.
     */
    boolean hasPhenotypicRelevance();
}
