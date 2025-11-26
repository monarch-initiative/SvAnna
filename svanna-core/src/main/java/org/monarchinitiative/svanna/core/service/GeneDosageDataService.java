package org.monarchinitiative.svanna.core.service;

import org.monarchinitiative.svanna.model.landscape.dosage.DosageRegion;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;

public interface GeneDosageDataService {

    List<DosageRegion> dosageElements(GenomicRegion query);

}
