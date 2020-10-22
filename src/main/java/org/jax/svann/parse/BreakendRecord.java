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
    private final byte[] ref;
    private final byte[] alt;

    public BreakendRecord(ChromosomalRegion position,
                          String id,
                          String mateId,
                          byte[] ref,
                          byte[] alt) {
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

    public byte[] getRef() {
        return ref;
    }

    public byte[] getAlt() {
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
                Arrays.equals(ref, that.ref) &&
                Arrays.equals(alt, that.alt);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(position, id, mateId);
        result = 31 * result + Arrays.hashCode(ref);
        result = 31 * result + Arrays.hashCode(alt);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)%s>%s", position, id, new String(ref), new String(alt));
    }
}
