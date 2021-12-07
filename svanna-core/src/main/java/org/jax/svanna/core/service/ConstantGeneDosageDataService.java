package org.jax.svanna.core.service;

import org.jax.svanna.model.landscape.dosage.*;
import org.monarchinitiative.svart.GenomicRegion;
import xyz.ielis.silent.genes.model.Gene;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This gene dosage data service assumes all genes are haploinsufficient and triplosensitive.
 */
public class ConstantGeneDosageDataService implements GeneDosageDataService {

    private static final List<Dosage> DEFAULT = List.of(
            Dosage.of("constant-triplosensitivity", DosageSensitivity.TRIPLOSENSITIVITY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE),
            Dosage.of("constant-haploinsufficiency", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE));

    private final GeneService geneService;

    public ConstantGeneDosageDataService(GeneService geneService) {
        this.geneService = geneService;
    }

    @Override
    public List<DosageRegion> dosageElements(GenomicRegion query) {
        return geneService.overlappingGenes(query).overlapping().stream()
                .map(toDosageRegion())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private Function<Gene, List<DosageRegion>> toDosageRegion() {
        return g -> List.of(
                DosageRegion.of(g.location(), DEFAULT.get(0)),
                DosageRegion.of(g.location(), DEFAULT.get(1))
        );
    }

    @Override
    public List<Dosage> geneDosageDataForHgncId(String hgncId) {
        return DEFAULT;
    }

    @Override
    public List<Dosage> geneDosageDataForHgncIdAndRegion(String hgncId, GenomicRegion query) {
        return DEFAULT;
    }
}
