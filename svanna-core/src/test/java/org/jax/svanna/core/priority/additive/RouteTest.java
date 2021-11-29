package org.jax.svanna.core.priority.additive;

import org.jax.svanna.core.TestContig;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

public class RouteTest {

    @Test
    public void metaSegments() {
        TestContig ctg1 = TestContig.of(0, 100);
        TestContig ctg2 = TestContig.of(1, 200);
        TestContig ctg3 = TestContig.of(2, 300);
        Route route = Route.of(
                List.of(
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 40, "gap-11", Event.GAP, 1),
                        Segment.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 40, 50, "gap-12", Event.GAP, 1),

                        Segment.of(ctg2, Strand.NEGATIVE, CoordinateSystem.zeroBased(), 120, 145, "gap-21", Event.GAP, 1),
                        Segment.of(ctg2, Strand.NEGATIVE, CoordinateSystem.zeroBased(), 145, 160, "gap-22", Event.GAP, 1),
                        Segment.of(ctg2, Strand.NEGATIVE, CoordinateSystem.zeroBased(), 160, 170, "gap-23", Event.GAP, 1),

                        Segment.of(ctg3, Strand.POSITIVE, CoordinateSystem.zeroBased(), 30, 40, "gap-31", Event.GAP, 1),
                        Segment.of(ctg3, Strand.POSITIVE, CoordinateSystem.zeroBased(), 40, 60, "gap-32", Event.GAP, 1)
                ));

        List<GenomicRegion> metaSegments = route.metaSegments();
        assertThat(metaSegments, hasSize(3));
        assertThat(metaSegments, hasItem(GenomicRegion.of(ctg1, Strand.POSITIVE, CoordinateSystem.zeroBased(), 20, 50)));
        assertThat(metaSegments, hasItem(GenomicRegion.of(ctg2, Strand.NEGATIVE, CoordinateSystem.zeroBased(), 120, 170)));
        assertThat(metaSegments, hasItem(GenomicRegion.of(ctg3, Strand.POSITIVE, CoordinateSystem.zeroBased(), 30, 60)));
    }
}