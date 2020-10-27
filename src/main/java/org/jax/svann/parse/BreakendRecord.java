package org.jax.svann.parse;

import org.jax.svann.reference.ChromosomalRegion;

import java.util.Arrays;
import java.util.Objects;

/**
 * Minimal information required to get from VCF breakend record.
 */
public class BreakendRecord {

    private final ChromosomalRegion position;
    private final String id;
    private final String mateId;
    private final String ref;
    private final String alt;

    public BreakendRecord(ChromosomalRegion position,
                          String id,
                          String mateId,
                          String ref,
                          String alt) {
        this.position = position;
        this.id = id;
        this.mateId = mateId;
        this.ref = ref;
        this.alt = alt;
    }

    public ChromosomalRegion getPosition() {
        return position;
    }

    public String getId() {
        return id;
    }

    public String getMateId() {
        return mateId;
    }

    public String getRef() {
        return ref;
    }

    public String getAlt() {
        return alt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreakendRecord that = (BreakendRecord) o;
        return Objects.equals(position, that.position) &&
                Objects.equals(id, that.id) &&
                Objects.equals(mateId, that.mateId) &&
                Objects.equals(ref, that.ref) &&
                Objects.equals(alt, that.alt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, id, mateId, ref, alt);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)%s>%s", position, id, ref, alt);
    }
}
