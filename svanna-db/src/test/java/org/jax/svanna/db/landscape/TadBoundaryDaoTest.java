package org.jax.svanna.db.landscape;

import org.jax.svanna.model.landscape.TadBoundary;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class TadBoundaryDaoTest extends AbstractDaoTest {

    @ParameterizedTest
    @CsvSource({
            "1, 30, 40, POSITIVE, 1", // `two` is not fetched because the midpoint does not overlap
            "1, 29, 40, POSITIVE, 2",
            "1, 10, 50, POSITIVE, 2", // `three` is not fetched due to low stability
    })
    @Sql({"tad_boundary_create_table.sql", "tad_boundary_insert_data.sql"})
    public void getOverlapping(int contigId, int start, int end, Strand strand, int size) {
        TadBoundaryDao dao = new TadBoundaryDao(dataSource, ASSEMBLY, .8);
        GenomicRegion region = GenomicRegion.of(ASSEMBLY.contigById(contigId), strand, CoordinateSystem.zeroBased(), start, end);

        List<TadBoundary> overlapping = dao.getOverlapping(region);

        assertThat(overlapping, hasSize(size));
    }

    @ParameterizedTest
    @CsvSource({
            "1,  30,  30, POSITIVE,    false,  0",
            "1,  40,  40, POSITIVE,     true, 30",
            "1,  41,  41, POSITIVE,     true, 40",
            "1, 500, 500, POSITIVE,     true, 80",
            "1,  30,  30, NEGATIVE,     true, 248956382",
    })
    @Sql({"tad_boundary_create_table.sql", "tad_boundary_insert_data.sql"})
    public void upstreamOf(int contigId, int start, int end, Strand strand,
                           boolean present, int pos) {
        TadBoundaryDao dao = new TadBoundaryDao(dataSource, ASSEMBLY, .8);
        GenomicRegion region = GenomicRegion.of(ASSEMBLY.contigById(contigId), Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end).withStrand(strand);

        Optional<TadBoundary> upstream = dao.upstreamOf(region);

        assertThat(upstream.isPresent(), equalTo(present));
        if (upstream.isPresent()) {
            assertThat(upstream.get().startWithCoordinateSystem(CoordinateSystem.zeroBased()), equalTo(pos));
            assertThat(upstream.get().endWithCoordinateSystem(CoordinateSystem.zeroBased()), equalTo(pos));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "1,  30,  30, POSITIVE,     true, 40",
            "1,  40,  40, POSITIVE,     true, 80",
            "1,  80,  80, POSITIVE,    false,  0",
            "1, 100, 100, NEGATIVE,     true, 248956342",
    })
    @Sql({"tad_boundary_create_table.sql", "tad_boundary_insert_data.sql"})
    public void downstreamOf(int contigId, int start, int end, Strand strand,
                             boolean present, int pos) {
        TadBoundaryDao dao = new TadBoundaryDao(dataSource, ASSEMBLY, .8);
        GenomicRegion region = GenomicRegion.of(ASSEMBLY.contigById(contigId), Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end).withStrand(strand);

        Optional<TadBoundary> upstream = dao.downstreamOf(region);

        assertThat(upstream.isPresent(), equalTo(present));
        if (upstream.isPresent()) {
            assertThat(upstream.get().startWithCoordinateSystem(CoordinateSystem.zeroBased()), equalTo(pos));
            assertThat(upstream.get().endWithCoordinateSystem(CoordinateSystem.zeroBased()), equalTo(pos));
        }
    }
}