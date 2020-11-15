package org.jax.svann.viz;

import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.transcripts.SvAnnTxModel;

import java.util.List;

public interface Visualizable {

    String getImpact();

    String getType();

    boolean hasPhenotypicRelevance();

    SequenceRearrangement getRearrangement();

    List<HtmlLocation> getLocations();

    List<HpoDiseaseSummary> getDiseaseSummaries();

    List<Overlap> getOverlaps();

    List<SvAnnTxModel> getTranscripts();
    /** @return the total number of genes affected by this structural variant. */
    int getGeneCount();

    List<Enhancer> getEnhancers();

}
