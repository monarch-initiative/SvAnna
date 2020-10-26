package org.jax.svann.priority;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.parse.SvEvent;
import org.jax.svann.reference.SvType;
import org.jax.svann.viz.Visualizable;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Set;

public class PrioritizedSv implements SvPriority, Visualizable {





    private PrioritizedSv(SvEvent e) {

    }



    public static PrioritizedSv fromSvEvent(SvEvent sve) {

        return new PrioritizedSv(sve);
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
    public SvType getSvType() {
        return null;
    }

    @Override
    public boolean hasPhenotypicRelevance() {
        return false;
    }

    @Override
    public SvPriority getPriority() {
        return null;
    }
}
