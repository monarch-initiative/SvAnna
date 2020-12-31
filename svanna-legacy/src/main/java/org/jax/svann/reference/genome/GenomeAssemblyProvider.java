package org.jax.svann.reference.genome;

import java.util.Optional;
import java.util.Set;

public interface GenomeAssemblyProvider {

    /**
     * @return provider with Homo sapiens genome assemblies <code>GRCh37.p13</code> and <code>GRCh38.p13</code>
     */
    static GenomeAssemblyProvider getDefaultProvider() {
        return DefaultGenomeAssemblyProvider.getInstance();
    }

    /**
     * Convenience method for getting the <code>GRCh38.p13</code>
     *
     * @return <code>GRCh38.p13</code> assembly
     */
    static GenomeAssembly getGrch38Assembly() {
        String assembly = "GRCh38.p13";
        return getDefaultProvider().getAssembly(assembly).orElseThrow(() -> new RuntimeException(String.format("Whoops, missing `%s` assembly", assembly)));
    }

    /**
     * Get genome assembly by assembly name.
     *
     * @param id assembly id, e.g <code>GRCh38.p13</code>
     * @return optional with the assembly or an empty
     */
    Optional<GenomeAssembly> getAssembly(String id);

    /**
     * @return set of genome assemblies that are provided by this provider
     */
    Set<String> getAssemblyIds();
}
