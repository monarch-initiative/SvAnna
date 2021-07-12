package org.jax.svanna.core.overlap;


/**
 * This class represents the first and last exons in a transcript that is affected by a structural variant. If
 * both {@link #firstAffectedExon} and {@link #lastAffectedExon} are -1, this is a flag that no exon overlapped.
 * @author Peter Robinson
 */
class ExonPair {

    private final int firstAffectedExon;
    private final int lastAffectedExon;

    ExonPair(int first, int second) {
        firstAffectedExon = first;
        lastAffectedExon = second;
    }

    boolean atLeastOneExonOverlap() {
        return firstAffectedExon>0 || lastAffectedExon>0;
    }

    int getFirstAffectedExon() {
        return firstAffectedExon;
    }

    int getLastAffectedExon() {
        return lastAffectedExon;
    }
}
