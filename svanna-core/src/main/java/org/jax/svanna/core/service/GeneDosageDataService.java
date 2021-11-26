package org.jax.svanna.core.service;

import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.dosage.GeneDosageData;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;

public interface GeneDosageDataService {

    List<DosageRegion> dosageElements(GenomicRegion query);

    GeneDosageData geneDosageDataForHgncId(String hgncId);

    GeneDosageData geneDosageDataForHgncIdAndRegion(String hgncId, GenomicRegion query);
}
