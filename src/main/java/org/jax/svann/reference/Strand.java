package org.jax.svann.reference;

public enum Strand {
    FWD("+"),
    REV("-");

    private final String name;

    Strand(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return name;
    }
}
