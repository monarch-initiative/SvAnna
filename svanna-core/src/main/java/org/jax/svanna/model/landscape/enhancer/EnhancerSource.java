package org.jax.svanna.model.landscape.enhancer;

public enum EnhancerSource {

    UNKNOWN(0),
    VISTA(1),
    FANTOM5(2);

    private final int id;

    EnhancerSource(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }
}
