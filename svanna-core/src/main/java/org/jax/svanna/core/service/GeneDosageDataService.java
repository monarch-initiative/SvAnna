package org.jax.svanna.core.service;

import org.jax.svanna.model.landscape.dosage.Dosage;
import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;

public interface GeneDosageDataService {

    List<DosageRegion> dosageElements(GenomicRegion query);

    List<Dosage> geneDosageDataForHgncId(String hgncId);

    List<Dosage> geneDosageDataForHgncIdAndRegion(String hgncId, GenomicRegion query);
}
