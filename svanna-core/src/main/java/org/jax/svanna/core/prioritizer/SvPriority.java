package org.jax.svanna.core.prioritizer;

import org.jax.svanna.core.hpo.GeneWithId;
import org.jax.svanna.core.hpo.HpoDiseaseSummary;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.reference.Enhancer;
import org.jax.svanna.core.reference.Transcript;

import java.util.List;
import java.util.Set;

public interface SvPriority extends CorePriority {

    static SvPriority unknown() {
        return DefaultSvPriority.unknown();
    }

    List<HpoDiseaseSummary> getDiseases();

    Set<Transcript> getAffectedTranscripts();

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
