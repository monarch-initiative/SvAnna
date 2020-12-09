package org.jax.svann.reference;

/**
 * Enum with selected elements of <em>GENO</em> ontology that represents subclasses of
 * <a href="http://www.ontobee.org/ontology/GENO?iri=http://purl.obolibrary.org/obo/GENO_0000391">disomic zygosity</a>.
 */
public enum Zygosity {

    /**
     * Corresponds to <em>no call</em>, unknown genotype
     */
    UNKNOWN,
    /**
     * <a href="http://www.ontobee.org/ontology/GENO?iri=http://purl.obolibrary.org/obo/GENO_0000136">Homozygous</a>
     */
    HOMOZYGOUS,

    /**
     * <a href="http://www.ontobee.org/ontology/GENO?iri=http://purl.obolibrary.org/obo/GENO_0000135">Heterozygous</a>
     */
    HETEROZYGOUS,

    /**
     * <a href="http://www.ontobee.org/ontology/GENO?iri=http://purl.obolibrary.org/obo/GENO_0000134">Hemizygous</a>
     */
    HEMIZYGOUS
}
