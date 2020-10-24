package org.jax.svann.reference;

public interface Breakend extends BreakendCoordinate {

    String getId();

    byte[] getRef();

    byte[] getInserted();
}
