package org.jax.svanna.core.landscape;

import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.Set;
import java.util.stream.Collectors;

public interface Enhancer extends GenomicRegion {

    String id();

    EnhancerSource enhancerSource();

    boolean isDevelopmental();

    Set<EnhancerTissueSpecificity> tissueSpecificity();

    @Override
    Enhancer withCoordinateSystem(CoordinateSystem coordinateSystem);

    @Override
    Enhancer withStrand(Strand other);

    double tau();

    default Set<TermId> hpoTermAssociations() {
        return tissueSpecificity().stream()
                .map(EnhancerTissueSpecificity::hpoTerm)
                .map(Term::getId)
                .collect(Collectors.toSet());
    }

}
