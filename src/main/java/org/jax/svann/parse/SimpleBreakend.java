package org.jax.svann.parse;

import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.ChromosomalRegion;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

import java.util.Objects;

class SimpleBreakend implements Breakend {

    private static final String EMPTY = "";

    private final ChromosomalRegion position;
    private final String id;
    private final String ref;

    static SimpleBreakend of(ChromosomalRegion position,
                       String id,
                       String ref) {
        return new SimpleBreakend(position, id, ref);
    }

    static SimpleBreakend of(ChromosomalRegion position,
                       String id) {
        return new SimpleBreakend(position, id, EMPTY);
    }

    private SimpleBreakend(ChromosomalRegion position,
                   String id,
                   String ref) {
        this.position = position;
        this.id = id;
        this.ref = ref;
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
        if (position.getStrand().equals(strand)) {
            return this;
        } else {
            return new SimpleBreakend(position.withStrand(strand), id, Utils.reverseComplement(ref));
        }
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleBreakend that = (SimpleBreakend) o;
        return Objects.equals(position, that.position) &&
                Objects.equals(id, that.id) &&
                Objects.equals(ref, that.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, id, ref);
    }

    @Override
    public String toString() {
        return "SimpleBreakend{" +
                "position=" + position +
                ", id='" + id + '\'' +
                ", ref='" + ref + '\'' +
                '}';
    }
}
