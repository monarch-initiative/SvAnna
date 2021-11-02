package org.jax.svanna.db.landscape;

import org.jax.svanna.model.landscape.PopulationVariant;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

public class DbPopulationVariantDaoTest extends AbstractDaoTest {


    @Test
    @Sql({"population_variants_create_table.sql", "population_variants_insert_data.sql"})
    public void getOverlapping() {
        DbPopulationVariantDao instance = new DbPopulationVariantDao(dataSource, ASSEMBLY);

        GenomicRegion query = GenomicRegion.of(ASSEMBLY.contigById(1), Strand.POSITIVE, CoordinateSystem.zeroBased(), 10, 21);
        List<PopulationVariant> overlapping = instance.getOverlapping(query);

        assertThat(overlapping, hasSize(1));
    }
}