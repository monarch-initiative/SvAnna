package org.jax.svanna.db.landscape;

import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

public class EnhancerAnnotationDaoTest extends AbstractDaoTest {

    @Test
    @Sql({"enhancer_create_table.sql", "enhancer_insert_data.sql"})
    public void getAllItems() {
        EnhancerAnnotationDao instance = new EnhancerAnnotationDao(dataSource, ASSEMBLY);
        List<Enhancer> items = instance.getAllItems();

        assertThat(items, hasSize(2));
        assertThat(items.stream().map(Enhancer::id).collect(Collectors.toSet()), hasItems("first", "second"));
    }
}