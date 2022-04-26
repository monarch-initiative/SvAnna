package org.monarchinitiative.svanna.core;

import org.monarchinitiative.svart.assembly.AssignedMoleculeType;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.assembly.SequenceRole;

public class TestContig implements Contig {

    private final int id;
    private final int length;

    public static TestContig of(int id, int length) {
        return new TestContig(id, length);
    }

    private TestContig(int id, int length) {
        this.id = id;
        this.length = length;
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
    public SequenceRole sequenceRole() {
        return SequenceRole.UNKNOWN;
    }

    @Override
    public String assignedMolecule() {
        return name();
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
}
