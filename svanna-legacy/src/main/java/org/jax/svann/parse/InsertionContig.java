package org.jax.svann.parse;

import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.SequenceRole;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class represents an anonymous contig for SV insertions.
 * <p>
 * A records like
 * <pre>2    15  INS0    T   <INS>   6   PASS    SVTYPE=INS;END=15;SVLEN=10</pre>
 * only specify that 10 bases were inserted at position 15. However, the bases are not further specified.
 * <p>
 * The purpose of this contig is to allow the rest of framework to work.
 */
class InsertionContig implements Contig {

    private static final AtomicInteger COUNTER = new AtomicInteger(50_000);

    private final int id;

    private final String name;

    private final int length;

    InsertionContig(String name, int length) {
        this.id = COUNTER.getAndIncrement();
        this.name = name;
        this.length = length;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getPrimaryName() {
        return name;
    }

    @Override
    public Set<String> getNames() {
        return Set.of(name);
    }

    @Override
    public SequenceRole getSequenceRole() {
        return SequenceRole.UNKNOWN;
    }

    @Override
    public int getLength() {
        return length;
    }
}
