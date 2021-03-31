package org.jax.svanna.core.priority;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public interface SvPrioritizerFactory {

     Logger LOGGER = LoggerFactory.getLogger(SvPrioritizerFactory.class);

    <V extends Variant> SvPrioritizer<V, SvPriority> getPrioritizer(SvPrioritizerType type, Collection<TermId> patientTerms);


}
