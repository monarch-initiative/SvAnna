package org.jax.svann.reference;

import java.util.HashSet;
import java.util.Set;

public interface Contig extends Comparable<Contig> {

    /**
     * @return numeric id that is unique within the {@link GenomeAssembly}
     */
    int getId();

    /**
     * @return primary name of the contig, e.g. "X"
     */
    String getPrimaryName();

    /**
     * @return all admissible names of the contig, e.g. "chrX", "X", "23"
     */
    Set<String> getNames();

    /**
     * @return contig sequence role
     */
    SequenceRole getSequenceRole();

    /**
     * @return number of bases of the contig
     */
    int getLength();


    @Override
    default int compareTo(Contig o) {
        return Integer.compare(getId(), o.getId());
    }

}
