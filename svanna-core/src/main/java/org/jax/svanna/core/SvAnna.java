package org.jax.svanna.core;

import org.jax.svanna.core.priority.SvPrioritizerFactory;
import org.jax.svanna.core.service.AnnotationDataService;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.core.service.PhenotypeDataService;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

import java.util.Objects;

public class SvAnna {

    private final GenomicAssembly assembly;

    private final GeneService geneService;
    private final PhenotypeDataService phenotypeDataService;

    private  final AnnotationDataService annotationDataService;
    private final SvPrioritizerFactory prioritizerFactory;

    public static SvAnna of(GenomicAssembly assembly,
                            GeneService geneService,
                            PhenotypeDataService phenotypeDataService,
                            AnnotationDataService annotationDataService,
                            SvPrioritizerFactory prioritizerFactory) {
        return new SvAnna(assembly,
                geneService,
                phenotypeDataService,
                annotationDataService,
                prioritizerFactory);
    }

    private SvAnna(GenomicAssembly assembly,
                  GeneService geneService,
                  PhenotypeDataService phenotypeDataService,
                  AnnotationDataService annotationDataService,
                  SvPrioritizerFactory prioritizerFactory) {
        this.assembly = Objects.requireNonNull(assembly);
        this.geneService = Objects.requireNonNull(geneService);
        this.phenotypeDataService = Objects.requireNonNull(phenotypeDataService);
        this.annotationDataService = Objects.requireNonNull(annotationDataService);
        this.prioritizerFactory = Objects.requireNonNull(prioritizerFactory);
    }

    public GenomicAssembly assembly() {
        return assembly;
    }

    public GeneService geneService() {
        return geneService;
    }

    public PhenotypeDataService phenotypeDataService() {
        return phenotypeDataService;
    }

    public AnnotationDataService annotationDataService() {
        return annotationDataService;
    }

    public SvPrioritizerFactory prioritizerFactory() {
        return prioritizerFactory;
    }
}
