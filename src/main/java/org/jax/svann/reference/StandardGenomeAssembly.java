package org.jax.svann.reference;

import java.util.Map;
import java.util.Objects;

class StandardGenomeAssembly implements GenomeAssembly {

    private final String id;

    private final String taxonId;

    private final Map<Integer, Contig> contigMap;

    private StandardGenomeAssembly(String id, String taxonId, Map<Integer, Contig> contigMap) {
        this.id = id;
        this.taxonId = taxonId;
        this.contigMap = Map.copyOf(contigMap);
    }

    public static StandardGenomeAssembly of(String id, String taxonId, Map<Integer, Contig> contigMap) {
        return new StandardGenomeAssembly(id, taxonId, contigMap);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTaxonId() {
        return taxonId;
    }

    @Override
    public Map<Integer, Contig> getContigMap() {
        return contigMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardGenomeAssembly that = (StandardGenomeAssembly) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(taxonId, that.taxonId) &&
                Objects.equals(contigMap, that.contigMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, taxonId, contigMap);
    }

    @Override
    public String toString() {
        return "GenomeAssembly{" +
                "id='" + id + '\'' +
                ", organismName='" + taxonId + '\'' +
                ", contigMap=" + contigMap +
                '}';
    }
}
