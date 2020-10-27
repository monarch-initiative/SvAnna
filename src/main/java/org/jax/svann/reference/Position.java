package org.jax.svann.reference;

import java.util.Comparator;
import java.util.Objects;

/**
 * Position with a confidence level expressed using a {@link ConfidenceInterval}.
 */
public class Position implements Comparable<Position> {

    private static final Comparator<Position> COMPARATOR = Comparator.comparing(Position::getPos)
            .thenComparing(Position::getConfidenceInterval);

    /**
     * 1-based position
     */
    private final int pos;

    private final ConfidenceInterval confidenceInterval;

    private Position(int pos, ConfidenceInterval confidenceInterval, CoordinateSystem coordinateSystem) {
        // convert to 1-based
        this.pos = coordinateSystem.equals(CoordinateSystem.ONE_BASED) ? pos : pos + 1;
        if (this.pos <= 0) {
            throw new IllegalArgumentException(String.format("One-based position `%d` cannot be non-positive", this.pos));
        }
        this.confidenceInterval = Objects.requireNonNull(confidenceInterval);
    }

    /**
     * Create precise position using given coordinate system.
     *
     * @param pos              position coordinate
     * @param coordinateSystem coordinate system
     * @return precise position
     */
    public static Position precise(int pos, CoordinateSystem coordinateSystem) {
        return imprecise(pos, ConfidenceInterval.precise(), coordinateSystem);
    }

    /**
     * Create 1-based precise position.
     *
     * @param pos position coordinate
     * @return precise position
     */
    public static Position precise(int pos) {
        return imprecise(pos, ConfidenceInterval.precise());
    }

    public static Position imprecise(int pos, ConfidenceInterval confidenceInterval, CoordinateSystem coordinateSystem) {
        return new Position(pos, confidenceInterval, coordinateSystem);
    }

    public static Position imprecise(int pos, ConfidenceInterval confidenceInterval) {
        return new Position(pos, confidenceInterval, CoordinateSystem.ONE_BASED);
    }

    /**
     * @return one based position
     */
    public int getPos() {
        return pos;
    }

    /**
     * @return confidence interval associated with the position
     */
    public ConfidenceInterval getConfidenceInterval() {
        return confidenceInterval;
    }

    /**
     * @return true if this position is precise (CI = [0,0])
     */
    public boolean isPrecise() {
        return confidenceInterval.isPrecise();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return pos == position.pos &&
                Objects.equals(confidenceInterval, position.confidenceInterval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, confidenceInterval);
    }

    @Override
    public String toString() {
        String ci = (confidenceInterval.isPrecise())
                ? ""
                : "(+-" + confidenceInterval + ")";
        return pos + ci;
    }

    @Override
    public int compareTo(Position o) {
        return COMPARATOR.compare(this, o);
    }
}
