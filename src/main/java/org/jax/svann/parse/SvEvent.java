package org.jax.svann.parse;

import org.jax.svann.reference.IntrachromosomalEvent;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.SvType;

import java.util.Objects;

@Deprecated
public class SvEvent implements IntrachromosomalEvent {

    private final Contig contig;

    private final Position begin, end;

    private final Strand strand;

    private final SvType type;

    private SvEvent(Contig contig,
                    Position begin,
                    Position end,
                    Strand strand,
                    SvType type) {
        this.contig = Objects.requireNonNull(contig);
        this.begin = Objects.requireNonNull(begin);
        this.end = Objects.requireNonNull(end);
        this.strand = Objects.requireNonNull(strand);
        this.type = Objects.requireNonNull(type);

        // checks
        if (end.getPos() > contig.getLength()) {
            // TODO(PNR): 26. 10. 2020 should we also throw an exception if CI bound is past the contig end?
            throw new IllegalArgumentException(String.format("End position `%d` past the contig end `%d`", end.getPos(), contig.getLength()));
        }
        if (begin.getPos() > end.getPos()) {
            throw new IllegalArgumentException(String.format("Begin position `%d` past the end position `%d`", begin.getPos(), end.getPos()));
        }
    }

    public static SvEvent of(Contig contig, Position begin, Position end, Strand strand, SvType type) {
        return new SvEvent(contig, begin, end, strand, type);
    }

    /**
     * Create event using precise begin and end coordinates (confidence intervals for both begin/end are [0,0])
     *
     * @return precise SV event
     */
    public static SvEvent precise(Contig contig, int begin, int end, Strand strand, SvType type) {
        return new SvEvent(contig, Position.precise(begin), Position.precise(end), strand, type);
    }


    @Override
    public SvType getType() {
        return type;
    }

    @Override
    public Contig getContig() {
        return contig;
    }

    @Override
    public Position getBeginPosition() {
        return begin;
    }

    @Override
    public Position getEndPosition() {
        return end;
    }

    @Override
    public Strand getStrand() {
        return strand;
    }

    @Override
    public SvEvent withStrand(Strand strand) {
        if (this.strand.equals(strand)) {
            return this;
        } else {
            Position begin = Position.imprecise(contig.getLength() - this.end.getPos() + 1,
                    this.end.getConfidenceInterval().toOppositeStrand());
            Position end = Position.imprecise(contig.getLength() - this.begin.getPos() + 1,
                    this.begin.getConfidenceInterval().toOppositeStrand());
            return new SvEvent(contig, begin, end, strand, type);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SvEvent svAnn = (SvEvent) o;
        return Objects.equals(contig, svAnn.contig) &&
                Objects.equals(begin, svAnn.begin) &&
                Objects.equals(end, svAnn.end) &&
                type == svAnn.type &&
                strand == svAnn.strand;
    }

    @Override
    public int hashCode() {
        return Objects.hash(contig, begin, end, type, strand);
    }

    @Override
    public String toString() {
        return "SvEvent{" +
                "contig=" + contig +
                ", begin=" + begin +
                ", end=" + end +
                ", type=" + type +
                ", strand=" + strand +
                '}';
    }
}
