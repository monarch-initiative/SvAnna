package org.jax.svanna.core.reference;

import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.svart.*;

import java.util.Set;
import java.util.stream.Collectors;

public interface Enhancer extends GenomicRegion {

    static Enhancer of(Contig contig, Strand strand, CoordinateSystem coordinateSystem, Position startPosition, Position endPosition,
                       String id, boolean isDevelopmental, double tau, Set<EnhancerTissueSpecificity> specificities) {
        return BaseEnhancer.of(contig, strand, coordinateSystem, startPosition, endPosition, id, isDevelopmental, tau, specificities);
    }

    String id();

    boolean isDevelopmental();

    Set<EnhancerTissueSpecificity> tissueSpecificity();

    double tau();

    default Set<TermId> hpoTermAssociations() {
        return tissueSpecificity().stream()
                .map(EnhancerTissueSpecificity::hpoTerm)
                .map(Term::getId)
                .collect(Collectors.toSet());
    }

}
