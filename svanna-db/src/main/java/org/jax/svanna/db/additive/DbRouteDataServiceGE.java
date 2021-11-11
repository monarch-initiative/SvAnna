package org.jax.svanna.db.additive;

import org.jax.svanna.core.priority.additive.RouteDataService;
import org.jax.svanna.core.priority.additive.Routes;
import org.jax.svanna.core.priority.additive.ge.RouteDataGE;
import org.jax.svanna.core.service.AnnotationDataService;
import org.jax.svanna.core.service.GeneService;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.tad.TadBoundary;
import org.monarchinitiative.svart.GenomicRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.ielis.silent.genes.model.Gene;
import xyz.ielis.silent.genes.model.Located;

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

        for (GenomicRegion reference : route.references()) {
            Predicate<? super Located> isContainedInRoute = r -> reference.contains(r.location());
            List<Gene> genes = geneService.overlappingGenes(reference).stream()
                    .filter(isContainedInRoute)
                    .collect(Collectors.toList());

            List<Enhancer> enhancers = annotationDataService.overlappingEnhancers(reference).stream()
                    .filter(isContainedInRoute)
                    .collect(Collectors.toList());

            List<TadBoundary> boundaries = annotationDataService.overlappingTadBoundaries(reference).stream()
                    .filter(isContainedInRoute)
                    .filter(notOverlappingWithGene(genes))
                    .collect(Collectors.toList());

            builder.addGenes(genes)
                    .addEnhancers(enhancers)
                    .addTadBoundaries(boundaries);
        }

        return builder.build();
    }

    private static Predicate<? super TadBoundary> notOverlappingWithGene(Collection<Gene> genes) {
        return tad -> genes.stream().noneMatch(g -> g.location().overlapsWith(tad.location()));
    }

}
