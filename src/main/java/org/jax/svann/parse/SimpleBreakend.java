package org.jax.svann.parse;

import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.ChromosomalRegion;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

public class SimpleBreakend implements Breakend {

    private final ChromosomalRegion position;
    private final String id;
    private final String ref, inserted;


    SimpleBreakend(ChromosomalRegion position,
                   String id, String ref,
                   String inserted) {
        this.position = position;
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
    public Breakend withStrand(Strand strand) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public String getInserted() {
        return inserted;
    }

    @Override
    public String toString() {
        return "SimpleBreakend{" +
                "position=" + position +
                ", id='" + id + '\'' +
                ", ref='" + ref + '\'' +
                ", inserted='" + inserted + '\'' +
                '}';
    }
}
