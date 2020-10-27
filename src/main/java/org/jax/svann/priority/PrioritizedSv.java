package org.jax.svann.priority;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.parse.SvEvent;
import org.jax.svann.reference.SvType;
import org.jax.svann.viz.Visualizable;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Set;

public class PrioritizedSv implements SvPriority, Visualizable {
    /** A structural variant that is being prioritized. */
    private final SvEvent svEvent;
    private final List<Overlap> overlaps;
    private final List<Enhancer> enhancers;
    private final List<HpoDiseaseSummary> diseases;

    private PrioritizedSv(SvEvent e, List<Overlap> overlaps, List<Enhancer> enhancers, List<HpoDiseaseSummary> diseases) {
        this.svEvent = e;
        this.overlaps = overlaps;
        this.enhancers = enhancers;
        this.diseases = diseases;

    }



    public static PrioritizedSv fromSvEvent(SvEvent sve, List<Overlap> overlaps, List<Enhancer> enhancers, List<HpoDiseaseSummary> diseases) {

        return new PrioritizedSv(sve, overlaps, enhancers, diseases);
    }


    @Override
    public SvImpact getImpact() {
        return null;
    }

    @Override
    public Set<TranscriptModel> getAffectedTranscripts() {
        return null;
    }

    @Override
    public Set<TermId> getAffectedGeneIds() {
        return null;
    }

    @Override
    public Set<Enhancer> getAffectedEnhancers() {
        return null;
    }

    @Override
    public SvImpact getSvImpact() {
        return null;
    }

    /** An SvEvent is phenotypically relevant if it is assigned to one or more diseases. */
    @Override
    public boolean hasPhenotypicRelevance() {
        return this.diseases.size() > 0;
    }

    @Override
    public SvPriority getPriority() {
        return null;
    }
}
