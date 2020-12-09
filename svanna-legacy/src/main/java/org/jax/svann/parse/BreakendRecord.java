package org.jax.svann.parse;


import java.util.Objects;

/**
 * Minimal information required to get from VCF breakend record.
 */
public class BreakendRecord {

    private final ChromosomalPosition position;
    private final String id;
    private final String eventId;
    private final String mateId;
    private final String ref;
    private final String alt;
    private final int depthOfCoverage;

    public BreakendRecord(ChromosomalPosition position,
                          String id,
                          String eventId,
                          String mateId,
                          String ref,
                          String alt,
                          int depthOfCoverage) {
        this.position = position;
        this.id = id;
        this.eventId = eventId;
        this.mateId = mateId;
        this.ref = ref;
        this.alt = alt;
        this.depthOfCoverage = depthOfCoverage;
    }

    public String getEventId() {
        return eventId;
    }

    public ChromosomalPosition getPosition() {
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

    public int depthOfCoverage() {
        return depthOfCoverage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BreakendRecord that = (BreakendRecord) o;
        return Objects.equals(position, that.position) &&
                Objects.equals(id, that.id) &&
                Objects.equals(eventId, that.eventId) &&
                Objects.equals(mateId, that.mateId) &&
                Objects.equals(ref, that.ref) &&
                Objects.equals(alt, that.alt) &&
                Objects.equals(depthOfCoverage, that.depthOfCoverage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, id, eventId, mateId, ref, alt, depthOfCoverage);
    }

    @Override
    public String toString() {
        return "BND " + id + " {" + position +
                ", EVENT='" + eventId + '\'' +
                ", MATE='" + mateId + '\'' +
                ", " + ref + '>' + alt + '}';
    }
}
