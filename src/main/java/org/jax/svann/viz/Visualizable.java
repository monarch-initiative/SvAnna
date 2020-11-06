package org.jax.svann.viz;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.reference.SequenceRearrangement;

import java.util.List;
import java.util.Map;

public interface Visualizable {

    String getImpact();

    String getType();

    boolean hasPhenotypicRelevance();

    SequenceRearrangement getRearrangement();

    List<HtmlLocation> getLocations();

    List<HpoDiseaseSummary> getDiseaseSummaries();

    List<Overlap> getOverlaps();

    List<TranscriptModel> getTranscripts();

    List<Enhancer> getEnhancers();

}
