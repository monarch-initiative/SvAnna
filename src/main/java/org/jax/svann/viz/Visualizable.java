package org.jax.svann.viz;

import org.jax.svann.priority.SvImpact;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.Adjacency;
import org.jax.svann.reference.SvType;

import java.util.List;
import java.util.Map;

public interface Visualizable {

    String getImpact();

    String getType();

    boolean hasPhenotypicRelevance();

    /**
     * key, value, where key is a display string such as 'location' and value is a string such
     * as chr12:4325-776623 (possibly a more intricate string involving a URL).
     * @return
     */
    Map<String, String> getLocationStrings();

}
