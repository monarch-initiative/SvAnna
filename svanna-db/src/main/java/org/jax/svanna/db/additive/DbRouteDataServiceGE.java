package org.jax.svanna.db.additive;

import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.landscape.TadBoundary;
import org.jax.svanna.core.priority.additive.RouteDataService;
import org.jax.svanna.core.priority.additive.Routes;
import org.jax.svanna.core.priority.additive.ge.RouteDataGE;
import org.jax.svanna.core.reference.Gene;
import org.jax.svanna.core.reference.GeneService;
import org.monarchinitiative.svart.GenomicRegion;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DbRouteDataServiceGE implements RouteDataService<RouteDataGE> {

    private final AnnotationDataService annotationDataService;
    private final GeneService geneService;

    public DbRouteDataServiceGE(AnnotationDataService annotationDataService, GeneService geneService) {
        this.annotationDataService = annotationDataService;
        this.geneService = geneService;
    }

    @Override
    public RouteDataGE getData(Routes route) {
        RouteDataGE.Builder builder = RouteDataGE.builder(route);

        Predicate<? super GenomicRegion> isContainedInRoute = e -> route.reference().contains(e);
        List<Gene> genes = geneService.overlappingGenes(route.reference()).stream()
                .filter(isContainedInRoute)
                .collect(Collectors.toList());
        List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(route.reference()).stream()
                .filter(isContainedInRoute)
                .collect(Collectors.toList());
        List<TadBoundary> boundaries = annotationDataService.overlappingTadBoundaries(route.reference()).stream()
                .filter(isContainedInRoute)
                .collect(Collectors.toList());

        builder.addRefGenes(genes)
                .addRefEnhancers(enhancers)
                .addRefTadBoundaries(boundaries);

        if (route.isIntraChromosomal()) {
            return builder.addAltGenes(genes)
                    .addAltEnhancers(enhancers)
                    .addAltTadBoundaries(boundaries)
                    .build();
        } else {
            List<GenomicRegion> metaSegments = route.alternate().metaSegments();
            if (metaSegments.size() == 1) {
                GenomicRegion segment = metaSegments.get(0);
                builder.addAltGenes(geneService.overlappingGenes(segment).stream()
                        .filter(segment::contains)
                        .collect(Collectors.toList()));
                builder.addAltEnhancers(annotationDataService.overlappingEnhancers(segment).stream()
                        .filter(segment::contains)
                        .collect(Collectors.toList()));
                builder.addAltTadBoundaries(annotationDataService.overlappingTadBoundaries(segment).stream()
                        .filter(segment::contains)
                        .collect(Collectors.toList()));
            } else {
                GenomicRegion first = metaSegments.get(0);
                builder.addAltGenes(geneService.overlappingGenes(first).stream()
                        .filter(first::contains)
                        .collect(Collectors.toList()));
                builder.addAltEnhancers(annotationDataService.overlappingEnhancers(first).stream()
                        .filter(first::contains)
                        .collect(Collectors.toList()));
                builder.addAltTadBoundaries(annotationDataService.overlappingTadBoundaries(first).stream()
                        .filter(first::contains)
                        .collect(Collectors.toList()));

                for (int i = 1, maxIdx = metaSegments.size()-1; i < maxIdx; i++) {
                    GenomicRegion metaSegment = metaSegments.get(i);
                    builder.addAltGenes(geneService.overlappingGenes(metaSegment));
                    builder.addAltEnhancers(annotationDataService.overlappingEnhancers(metaSegment));
                    builder.addAltTadBoundaries(annotationDataService.overlappingTadBoundaries(metaSegment));
                }

                GenomicRegion last = metaSegments.get(metaSegments.size() - 1);
                builder.addAltGenes(geneService.overlappingGenes(last).stream()
                        .filter(last::contains)
                        .collect(Collectors.toList()));
                builder.addAltEnhancers(annotationDataService.overlappingEnhancers(last).stream()
                        .filter(last::contains)
                        .collect(Collectors.toList()));
                builder.addAltTadBoundaries(annotationDataService.overlappingTadBoundaries(last).stream()
                        .filter(last::contains)
                        .collect(Collectors.toList()));
            }
            return builder.build();
        }
    }

}