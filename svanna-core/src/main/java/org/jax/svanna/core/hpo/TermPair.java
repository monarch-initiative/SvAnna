package org.jax.svanna.core.hpo;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

public class TermPair {

    private final TermId left;
    private final TermId right;

    private TermPair(TermId left, TermId right) {
        this.left = left;
        this.right = right;
    }

    public static TermPair of(TermId left, TermId right) {
        return new TermPair(left, right);
    }

    public static TermPair symmetric(TermId a, TermId b) {
        if (a.getId().compareTo(b.getId()) > 0) {
            return new TermPair(a, b);
        } else {
            return new TermPair(b, a);
        }
    }

    public static TermPair asymmetric(TermId a, TermId b) {
        return new TermPair(a, b);
    }

    public TermId left() {
        return left;
    }

    public TermId right() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TermPair termPair = (TermPair) o;
        return Objects.equals(left, termPair.left) && Objects.equals(right, termPair.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    @Override
    public String toString() {
        return "TermPair{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }
}
