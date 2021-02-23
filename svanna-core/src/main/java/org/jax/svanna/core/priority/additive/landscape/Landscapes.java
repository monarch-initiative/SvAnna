package org.jax.svanna.core.priority.additive.landscape;

import java.util.List;

@Deprecated(forRemoval = true)
public interface Landscapes<T extends Landscape> {

    List<T> reference();

    List<T> alternate();

}
