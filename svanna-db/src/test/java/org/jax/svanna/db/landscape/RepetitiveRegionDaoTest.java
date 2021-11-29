package org.jax.svanna.db.landscape;

import org.jax.svanna.model.landscape.repeat.RepetitiveRegion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class RepetitiveRegionDaoTest extends AbstractDaoTest {


    @Test
    @Sql({"repetitive_regions_create_table.sql", "repetitive_regions_insert_data.sql"})
    public void getAllItems() {
        RepetitiveRegionDao instance = new RepetitiveRegionDao(dataSource, ASSEMBLY);
        List<RepetitiveRegion> items = instance.getAllItems();
        assertThat(items, hasSize(3));
    }

    @ParameterizedTest
    @CsvSource({
            "10, 30, 1",
            "10, 31, 2",
            "30, 31, 1"
    })
    @Sql({"repetitive_regions_create_table.sql", "repetitive_regions_insert_data.sql"})
    public void getOverlapping(int start, int end, int count) {
        RepetitiveRegionDao instance = new RepetitiveRegionDao(dataSource, ASSEMBLY);

        GenomicRegion query = GenomicRegion.of(ASSEMBLY.contigById(1), Strand.POSITIVE, CoordinateSystem.zeroBased(), start, end);
        List<RepetitiveRegion> items = instance.getOverlapping(query);

        assertThat(items, hasSize(count));
    }
}