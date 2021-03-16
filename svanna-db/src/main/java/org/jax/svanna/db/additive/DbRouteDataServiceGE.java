package org.jax.svanna.db.additive;

import org.jax.svanna.core.exception.LogUtils;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.priority.additive.RouteDataService;
import org.jax.svanna.core.priority.additive.Routes;
import org.jax.svanna.core.priority.additive.ge.RouteDataGE;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.GeneService;
import org.monarchinitiative.svart.GenomicRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DbRouteDataServiceGE implements RouteDataService<RouteDataGE> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbRouteDataServiceGE.class);

    private final AnnotationDataService annotationDataService;
    private final GeneService geneService;

    public DbRouteDataServiceGE(AnnotationDataService annotationDataService, GeneService geneService) {
        this.annotationDataService = annotationDataService;
        this.geneService = geneService;
    }

    @Override
    public RouteDataGE getData(Routes route) {
        RouteDataGE.Builder builder = RouteDataGE.builder(route);

        GenomicRegion reference = route.reference();
        Predicate<? super GenomicRegion> isContainedInRoute = reference::contains;
        List<Gene> referenceGenes = geneService.overlappingGenes(reference).stream()
                .filter(isContainedInRoute)
                .collect(Collectors.toList());
        builder.addRefGenes(referenceGenes);

        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(reference).stream()
                .filter(isContainedInRoute)
                .collect(Collectors.toList());
        builder.addRefEnhancers(enhancers);

        List<TadBoundary> boundaries = annotationDataService.overlappingTadBoundaries(reference).stream()
                .filter(isContainedInRoute)
                .filter(notOverlappingWithGene(referenceGenes))
                .collect(Collectors.toList());
        builder.addRefTadBoundaries(boundaries);

        if (route.isIntraChromosomal()) {
            // the ALT genes, enhancers, and TADs are the same as for the REF
            return builder.addAltGenes(referenceGenes)
                    .addAltEnhancers(enhancers)
                    .addAltTadBoundaries(boundaries)
                    .build();
        } else {
            List<GenomicRegion> metaSegments = route.alternate().metaSegments();
            if (metaSegments.size() <= 1) {
                // Interchromosomal route consists of segments on >1 contigs by definition
                LogUtils.logWarn(LOGGER, "Interchromosomal route but only {} meta segment(s). This should not happen..", metaSegments.size());
                if (metaSegments.size() == 1) {
                    GenomicRegion segment = metaSegments.get(0);
                    getDataForAltSegment(segment, builder);
                }
            } else {
                GenomicRegion first = metaSegments.get(0);
                getDataForAltSegment(first, builder);

                for (int i = 1, maxIdx = metaSegments.size() - 1; i < maxIdx; i++) {
                    GenomicRegion metaSegment = metaSegments.get(i);
                    List<Gene> genes = geneService.overlappingGenes(metaSegment);
                    builder.addAltGenes(genes);
                    builder.addAltEnhancers(annotationDataService.overlappingEnhancers(metaSegment));
                    builder.addAltTadBoundaries(annotationDataService.overlappingTadBoundaries(metaSegment).stream()
                            .filter(notOverlappingWithGene(genes))
                            .collect(Collectors.toList())
                    );
                }

                GenomicRegion last = metaSegments.get(metaSegments.size() - 1);
                getDataForAltSegment(last, builder);
            }
            return builder.build();
        }
    }

    private void getDataForAltSegment(GenomicRegion segment, RouteDataGE.Builder builder) {
        List<Gene> genes = geneService.overlappingGenes(segment);
        builder.addAltGenes(genes.stream()
                .filter(segment::contains)
                .collect(Collectors.toList()));
        builder.addAltEnhancers(annotationDataService.overlappingEnhancers(segment).stream()
                .filter(segment::contains)
                .collect(Collectors.toList()));
        builder.addAltTadBoundaries(annotationDataService.overlappingTadBoundaries(segment).stream()
                .filter(segment::contains)
                .filter(notOverlappingWithGene(genes))
                .collect(Collectors.toList()));
    }

    private Predicate<? super TadBoundary> notOverlappingWithGene(Collection<Gene> genes) {
        return tad -> genes.stream().noneMatch(g -> g.overlapsWith(tad));
    }

}