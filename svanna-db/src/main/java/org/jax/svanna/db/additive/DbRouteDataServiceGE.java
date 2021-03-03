package org.jax.svanna.db.additive;

import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.priority.additive.RouteDataService;
import org.jax.svanna.core.priority.additive.Routes;
import org.jax.svanna.core.priority.additive.ge.RouteDataGE;
import org.jax.svanna.core.reference.GeneService;
import org.monarchinitiative.svart.GenomicRegion;

public class DbRouteDataServiceGE implements RouteDataService<RouteDataGE> {

    private final AnnotationDataService annotationDataService;
    private final GeneService geneService;

    public DbRouteDataServiceGE(AnnotationDataService annotationDataService, GeneService geneService) {
        this.annotationDataService = annotationDataService;
        this.geneService = geneService;
    }

    @Override
    public RouteDataGE getData(Routes route) {
        return (route.isIntraChromosomal())
                ? assembleIntraChromosomalRouteData(route)
                : assembleInterChromosomalRouteData(route);
    }

    private RouteDataGE assembleIntraChromosomalRouteData(Routes route) {
        RouteDataGE.Builder builder = RouteDataGE.builder(route);

        return builder.addAllGenes(geneService.overlappingGenes(route.reference()))
                .addAllEnhancers(annotationDataService.overlappingEnhancers(route.reference()))
                .addAllTadBoundaries(annotationDataService.overlappingTadBoundaries(route.reference()))
                .build();
    }

    private RouteDataGE assembleInterChromosomalRouteData(Routes route) {
        RouteDataGE.Builder builder = RouteDataGE.builder(route);

        for (GenomicRegion metaSegment : route.alternate().metaSegments()) {
            builder.addAllGenes(geneService.overlappingGenes(metaSegment));
            builder.addAllEnhancers(annotationDataService.overlappingEnhancers(metaSegment));
            builder.addAllTadBoundaries(annotationDataService.overlappingTadBoundaries(metaSegment));
        }

        return builder.build();
    }
}
