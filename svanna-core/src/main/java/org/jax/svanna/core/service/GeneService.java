package org.jax.svanna.core.service;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.sgenes.model.Gene;
import org.monarchinitiative.sgenes.model.Located;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;

public interface GeneService {

    /**
     * Most of the time, the list will have zero or one genes inside.
     * However, because of pseudo-autosomal regions, some genes can actually be on >1 contig, and the list will contain >1 genes.
     */
    List<Gene> byHgncId(TermId hgncId);

    QueryResult<Gene> overlappingGenes(GenomicRegion query);

    default QueryResult<Gene> overlappingGenes(Located located) {
        return overlappingGenes(located.location());
    }
}
