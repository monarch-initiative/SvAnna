package org.jax.svanna.model.landscape.enhancer;

import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import xyz.ielis.silent.genes.model.Located;

import java.util.Set;
import java.util.stream.Collectors;

public interface Enhancer extends Located {

    String id();

    EnhancerSource enhancerSource();

    boolean isDevelopmental();

    Set<EnhancerTissueSpecificity> tissueSpecificity();

    double tau();

    default Set<TermId> hpoTermAssociations() {
        return tissueSpecificity().stream()
                .map(EnhancerTissueSpecificity::hpoTerm)
                .map(Term::getId)
                .collect(Collectors.toUnmodifiableSet());
    }

}