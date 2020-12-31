package org.jax.svann.reference.genome;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface GenomeAssembly {

    /**
     * @return genome assembly id, e.g. <code>GRCh38.p13</code>
     */
    String getId();

    /**
     * @return taxon id, e.g. <em>9606</em> for Homo sapiens
     */
    String getTaxonId();

    /**
     * @return map with all contigs of the genome grouped by the contig ID
     */
    Map<Integer, Contig> getContigMap();

    /**
     * @param id contig id
     * @return optional with contig or an empty optional if the contig with given <code>id</code> is not present in
     * this genome build
     */
    default Optional<Contig> getContigById(int id) {
        return getContigMap().containsKey(id)
                ? Optional.of(getContigMap().get(id))
                : Optional.empty();
    }

    /**
     * Get contig by name.
     *
     * @param name primary or secondary name of a contig (e.g. "CM000663.1", "NC_000001.10", "chr1")
     * @return optional with contig or empty optional if contig with <code>name</code> is not present
     */
    default Optional<Contig> getContigByName(String name) {
        return getContigMap().values().stream()
                .filter(e -> e.getPrimaryName().equals(name) || e.getNames().contains(name))
                .findFirst();
    }


    default Set<Integer> getContigIds() {
        return Set.copyOf(getContigMap().keySet());
    }

    default Set<String> getContigNames() {
        return Stream.concat(
                getContigMap().values().stream().map(Contig::getPrimaryName), // primary names
                getContigMap().values().stream() // alt names
                        .map(Contig::getNames)
                        .flatMap(Collection::stream))
                .collect(Collectors.toSet());
    }

    /**
     * @return get sum of lengths of all contigs of this genome build
     */
    default long getLength() {
        return getContigMap().values().stream()
                .mapToLong(Contig::getLength)
                .sum();
    }
}
