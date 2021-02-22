package org.jax.svanna.core.priority;

import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.overlap.Overlap;
import org.jax.svanna.core.overlap.Overlapper;
import org.jax.svanna.core.reference.SvannaVariant;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ResnikPlusSequencePrioritier implements SvPrioritizer<SvannaVariant, SvPriority> {

    private final Overlapper overlapper;
    private final PhenotypeDataService dataService;
    private final Set<TermId> patientHpoTerms;
    
    
    public ResnikPlusSequencePrioritier(PhenotypeDataService phenotypeDataService, 
                                        Overlapper overlapper,
                                        Set<TermId> patientHpoTerms) {
        this.overlapper = overlapper;
        this.dataService = phenotypeDataService;
        this.patientHpoTerms = patientHpoTerms;
    } 
    
    
    @Override
    public SvPriority prioritize(SvannaVariant variant) {
        List<Overlap> overlaps = overlapper.getOverlaps(variant);
        List<String> symbols = overlaps.stream().map(Overlap::getGeneSymbol).distinct().collect(Collectors.toList());

        return null;
    }
}
