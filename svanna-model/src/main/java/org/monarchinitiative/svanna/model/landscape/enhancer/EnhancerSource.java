package org.monarchinitiative.svanna.model.landscape.enhancer;

public enum EnhancerSource {

    /*
    The ID field constraints:
      - do not reuse the same ID for different enhancer sources
     */

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
