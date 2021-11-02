package org.jax.svanna.core.priority.additive.impact;

//import org.jax.svanna.core.priority.additive.Projection;
//import org.jax.svanna.model.gene.Gene;

import org.jax.svanna.core.priority.additive.Projection;
import org.jax.svanna.model.gene.Gene;

public class GeneDosageAwareSequenceImpactCalculator implements SequenceImpactCalculator<Gene> {



    @Override
    public double projectImpact(Projection<Gene> projection) {
        /*
        This method returns
        - 0 if and only if the gene is haploinsufficient and the variant is LoF deletion.
        - 2..n if and only if the gene is triplosensitive and the variant is a duplication
        - 1 otherwise

        We need to be extra cautious
         */

        return 0;
    }
}
