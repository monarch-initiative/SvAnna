package org.jax.svann.reference.genome;

import java.util.Objects;
import java.util.Set;

/**
 * A simple {@link Contig} implementation.
 */
class StandardContig implements Contig {

    private final int id;

    private final String primaryName;

    private final Set<String> altNames;

    private final SequenceRole role;

    private final int length;

    private StandardContig(int id, String primaryName, Set<String> altNames, SequenceRole role, int length) {
        this.id = id;
        this.primaryName = primaryName;
        this.altNames = altNames;
        this.role = role;
        this.length = length;
    }

    public static StandardContig of(int id, String primaryName, Set<String> altNames, SequenceRole role, int length) {
        return new StandardContig(id, primaryName, altNames, role, length);
    }

    public static StandardContig of(int id, String primaryName, Set<String> altNames, int length) {
        return new StandardContig(id, primaryName, altNames, SequenceRole.ASSEMBLED_MOLECULE, length);
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
        return altNames;
    }

    @Override
    public SequenceRole getSequenceRole() {
        return role;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StandardContig contig = (StandardContig) o;
        return id == contig.id &&
                length == contig.length &&
                Objects.equals(primaryName, contig.primaryName) &&
                Objects.equals(altNames, contig.altNames) &&
                role == contig.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, primaryName, altNames, role, length);
    }

    @Override
    public String toString() {
        return String.format("%s(%d)", primaryName, id);
    }
}
