package org.monarchinitiative.svanna.core.priority.additive;

import org.monarchinitiative.svart.assembly.AssignedMoleculeType;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.assembly.SequenceRole;

import java.util.Objects;

/**
 * Contig implementation that belongs to a {@link Route}.
 */
class NeoContig implements Contig {

    private final int id;
    private final String name;
    private final int length;

    static NeoContig of(int id, String name, int length) {
        return new NeoContig(id, name, length);
    }

    private NeoContig(int id, String name, int length) {
        this.id = id;
        this.name = name;
        this.length = length;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public SequenceRole sequenceRole() {
        return SequenceRole.UNKNOWN;
    }

    @Override
    public String assignedMolecule() {
        return name;
    }

    @Override
    public AssignedMoleculeType assignedMoleculeType() {
        return AssignedMoleculeType.SEGMENT;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public String genBankAccession() {
        return "N/A";
    }

    @Override
    public String refSeqAccession() {
        return "N/A";
    }

    @Override
    public String ucscName() {
        return "N/A";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NeoContig neoContig = (NeoContig) o;
        return id == neoContig.id && length == neoContig.length && Objects.equals(name, neoContig.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, length);
    }

    @Override
    public String toString() {
        return "NeoContig{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", length=" + length +
                '}';
    }
}
