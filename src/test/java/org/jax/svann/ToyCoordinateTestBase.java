package org.jax.svann;

import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;

import java.util.Map;

public class ToyCoordinateTestBase {

    protected static GenomeAssembly TOY_ASSEMBLY = new ToyAssembly();

    private static class ToyAssembly implements GenomeAssembly {

        @Override
        public String getId() {
            return "toy";
        }

        @Override
        public String getTaxonId() {
            return "9606";
        }

        @Override
        public Map<Integer, Contig> getContigMap() {
            return Map.of(
                    1, new ContigImpl(1, "ctg1", 30),
                    2, new ContigImpl(2, "ctg2", 20)
            );
        }
    }

}
