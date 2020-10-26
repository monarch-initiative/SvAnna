package org.jax.svann;

import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.SequenceRole;

import java.util.Objects;
import java.util.Set;

/**
 * Contig implementation for testing only.
 */
public class ContigImpl implements Contig {

    private final int id;

    private final String primaryName;

    private final int length;

    public ContigImpl(int id, String primaryName, int length) {
        this.id = id;
        this.primaryName = primaryName;
        this.length = length;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getPrimaryName() {
        return primaryName;
    }

    @Override
    public Set<String> getNames() {
        return Set.of(primaryName);
    }

    @Override
    public SequenceRole getSequenceRole() {
        return SequenceRole.ASSEMBLED_MOLECULE;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContigImpl that = (ContigImpl) o;
        return id == that.id &&
                length == that.length &&
                Objects.equals(primaryName, that.primaryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, primaryName, length);
    }

    @Override
    public String toString() {
        return "ContigImpl{" +
                "id=" + id +
                ", primaryName='" + primaryName + '\'' +
                ", length=" + length +
                '}';
    }
}
