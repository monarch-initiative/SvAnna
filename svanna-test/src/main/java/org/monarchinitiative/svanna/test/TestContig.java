package org.monarchinitiative.svanna.test;

import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.assembly.AssignedMoleculeType;
import org.monarchinitiative.svart.assembly.SequenceRole;

import java.util.Objects;

public class TestContig implements Contig {

    private final int id;
    private final int length;

    public TestContig(int id, int length) {
        this.id = id;
        this.length = length;
    }

    public static TestContig of(int id, int length) {
        return new TestContig(id, length);
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String name() {
        return String.valueOf(id);
    }

    @Override
    public String assignedMolecule() {
        return name();
    }

    @Override
    public AssignedMoleculeType assignedMoleculeType() {
        return AssignedMoleculeType.UNKNOWN;
    }

    @Override
    public SequenceRole sequenceRole() {
        return SequenceRole.UNKNOWN;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public String genBankAccession() {
        return "";
    }

    @Override
    public String refSeqAccession() {
        return "";
    }

    @Override
    public String ucscName() {
        return "chr" + name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestContig)) return false;
        TestContig that = (TestContig) o;
        return id == that.id &&
                length == that.length;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, length);
    }

    @Override
    public String toString() {
        return "TestContig{" +
                "id=" + id +
                ", length=" + length +
                '}';
    }
}
