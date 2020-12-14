package org.jax.svann.reference;

public enum Strand {
    FWD("+"),
    REV("-");

    private final String name;

    Strand(String name) {
        this.name = name;
    }

    public boolean isForward() {
        return this == FWD;
    }

    public boolean isReverse() {
        return this == REV;
    }

    public Strand getOpposite() {
        return isForward()
                ? REV
                : FWD;
    }

    @Override
    public String toString() {
        return name;
    }
}
