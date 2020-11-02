package org.jax.svann.viz;

import org.jax.svann.hpo.HpoDiseaseSummary;
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

}
