package org.jax.svann.parse;

import org.jax.svann.reference.Breakend;
import org.jax.svann.reference.ConfidenceInterval;
import org.jax.svann.reference.CoordinateSystem;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;

import java.util.Objects;

class BreakendDefault implements Breakend {

    private static final String EMPTY = "";

    private final Contig contig;
    private final int position;
    private final ConfidenceInterval ci;
    private final Strand strand;
    private final CoordinateSystem coordinateSystem;
    private final String id;
    private final String ref;

    private BreakendDefault(Contig contig,
                            int position,
                            ConfidenceInterval ci,
                            Strand strand,
                            CoordinateSystem coordinateSystem,
                            String id,
                            String ref) {
        this.contig = contig;
        this.position = position;
        this.ci = ci;
        this.strand = strand;
        this.coordinateSystem = coordinateSystem;
        this.id = id;
        this.ref = ref;

    }

    static BreakendDefault preciseWithRef(Contig contig,
                                          int position,
                                          Strand strand,
                                          String id,
                                          String ref) {
        return new BreakendDefault(contig, position, ConfidenceInterval.precise(), strand, CoordinateSystem.ONE_BASED, id, ref);
    }

    static BreakendDefault precise(Contig contig,
                                   int position,
                                   Strand strand,
                                   String id) {
        return new BreakendDefault(contig, position, ConfidenceInterval.precise(), strand, CoordinateSystem.ONE_BASED, id, EMPTY);
    }

    static BreakendDefault impreciseWithRef(Contig contig,
                                            int position,
                                            ConfidenceInterval ci,
                                            Strand strand,
                                            CoordinateSystem coordinateSystem,
                                            String id,
                                            String ref) {
        return new BreakendDefault(contig, position, ci, strand, coordinateSystem, id, ref);
    }

    static BreakendDefault imprecise(Contig contig,
                                     int position,
                                     ConfidenceInterval ci,
                                     Strand strand,
                                     String id) {
        return new BreakendDefault(contig, position, ci, strand, CoordinateSystem.ONE_BASED, id, EMPTY);
    }

    @Override
    public Contig getContig() {
        return contig;
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public ConfidenceInterval getCi() {
        return ci;
    }

    @Override
    public CoordinateSystem getCoordinateSystem() {
        return CoordinateSystem.ONE_BASED;
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    @Override
    public BreakendDefault withStrand(Strand strand) {
        if (this.strand.equals(strand)) {
            return this;
        } else {
            int pos = contig.getLength() - position + 1;
            return new BreakendDefault(contig, pos, ci.toOppositeStrand(), strand, coordinateSystem, id, Utils.reverseComplement(ref));
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
        BreakendDefault that = (BreakendDefault) o;
        return position == that.position &&
                Objects.equals(contig, that.contig) &&
                Objects.equals(ci, that.ci) &&
                strand == that.strand &&
                coordinateSystem == that.coordinateSystem &&
                Objects.equals(id, that.id) &&
                Objects.equals(ref, that.ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contig, position, ci, strand, coordinateSystem, id, ref);
    }

    @Override
    public String toString() {
        return "BND(" + id + ")[" + contig + ":" + position + "(" + strand + ")" +
                "'" + ref + "']";
    }
}
