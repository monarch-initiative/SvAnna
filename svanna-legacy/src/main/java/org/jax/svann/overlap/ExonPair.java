package org.jax.svann.overlap;


/**
 * This class represents the first and last exons in a transcript that is affected by a structural variant. If
 * both {@link #firstAffectedExon} and {@link #lastAffectedExon} are -1, this is a flag that no exon overlapped.
 * @author Peter Robinson
 */
public class ExonPair {

    private final int firstAffectedExon;
    private final int lastAffectedExon;

    public ExonPair(int first, int second) {
        firstAffectedExon = first;
        lastAffectedExon = second;
    }

    public boolean atLeastOneExonOverlap() {
        return firstAffectedExon>0 || lastAffectedExon>0;
    }

    public int getFirstAffectedExon() {
        return firstAffectedExon;
    }

    public int getLastAffectedExon() {
        return lastAffectedExon;
    }
}
