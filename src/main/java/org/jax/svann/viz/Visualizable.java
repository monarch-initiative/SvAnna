package org.jax.svann.viz;

import org.jax.svann.reference.SequenceRearrangement;
import java.util.Map;

public interface Visualizable {

    String getImpact();

    String getType();

    boolean hasPhenotypicRelevance();

    SequenceRearrangement getRearrangement();

    /**
     * key, value, where key is a display string such as 'location' and value is a string such
     * as chr12:4325-776623 (possibly a more intricate string involving a URL).
     * @return
     */
    Map<String, String> getLocationStrings();

}
