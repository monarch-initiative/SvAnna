package org.jax.svann.parse;

import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;

import java.util.Arrays;

public class SimpleBreakend implements Breakend {

    private final ChromosomalRegion position;
    private final BreakendDirection direction;
    private final String id;
    private final byte[] ref, inserted;

    public SimpleBreakend(ChromosomalRegion position,
                          BreakendDirection direction,
                          String id, byte[] ref,
                          byte[] inserted) {
        this.position = position;
        this.direction = direction;
        this.id = id;
        this.ref = ref;
        this.inserted = inserted;
    }

    @Override
    public Contig getContig() {
        return position.getContig();
    }

    @Override
    public Position getBeginPosition() {
        return position.getBeginPosition();
    }

    @Override
    public Position getEndPosition() {
        return position.getEndPosition();
    }

    @Override
    public Strand getStrand() {
        return position.getStrand();
    }

    @Override
    public BreakendDirection getDirection() {
        return direction;
    }

    @Override
    public Breakend withStrand(Strand strand) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public byte[] getRef() {
        return ref;
    }

    @Override
    public byte[] getInserted() {
        return inserted;
    }

    @Override
    public String toString() {
        return "SimpleBreakend{" +
                "position=" + position +
                ", direction=" + direction +
                ", id='" + id + '\'' +
                ", ref=" + Arrays.toString(ref) +
                ", inserted=" + Arrays.toString(inserted) +
                '}';
    }
}
