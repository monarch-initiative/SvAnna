package org.jax.svanna.core.landscape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

/**
 * This enum describes sources of {@link PopulationVariant}s that we use within the app.
 */
public enum PopulationVariantOrigin {

    /**
     * Structural variants present in the GnomAD database.
     * <p>
     * Download section is <a href="https://gnomad.broadinstitute.org/downloads">here</a>.
     * </p>
     */
    GNOMAD_SV,

    /**
     * Structural variant data present in the <a href="http://dgv.tcag.ca/dgv/app/about">Database of Genomic Variants</a> (DGV) database.
     * <p>
     * Download section is <a href="http://dgv.tcag.ca/dgv/app/downloads?ref=">here</a>.
     * </p>
     */
    DGV,

    /**
     * Download section is <a href="http://ftp.1000genomes.ebi.ac.uk/vol1/ftp/data_collections/HGSVC2/release/v1.0/integrated_callset/">here</a>
     */
    HGSVC2,

    /**
     * CNV syndrome data available from the <a href="https://decipher.sanger.ac.uk/disorders#syndromes/overview">DECIPHER project</a>.
     * <p>
     * Download section is <a href="https://decipher.sanger.ac.uk/about#downloads/data">here</a>.
     * </p>
     */
    DECIPHER,

    /**
     * International Standards for Cytogenomic Arrays (ISCA) containing CNV data from chromosomal microarray analysis.
     * <p>
     * This aggregated and de‐identified clinical data, collected from laboratories around the world, is stored in
     * a publicly available database (http://www.ncbi.nlm.nih.gov/dbvar/studies/nstd37/; www.iscaconsortium.org).
     * </p>
     */
    ISCA,

    /**
     * <a href="https://www.ncbi.nlm.nih.gov/dbvar">dbVar</a> is NCBI's database of human genomic structural variation —
     * insertions, deletions, duplications, inversions, mobile elements, translocations, and others.
     *
     * <p>
     * This source represents regions of the genome that a submitter has defined as containing structural
     * variation.<br>
     * </p>
     * <p>
     * Very little meta-data is contained on these objects, as they are meant to provide a mark on the genome
     * to define regions containing variation. Variant regions point to sets of exemplar variant instances which support
     * the assertion that the region contains variation.
     * </p>
     * <p>
     * <a href="https://www.ncbi.nlm.nih.gov/dbvar/content/ftp_manifest/">FTP downloads</a>, data available for both
     * GRCh37 and GRCh38 assemblies in VCF format.
     * </p>
     */
    DBVAR_REGION,

    /**
     * <a href="https://www.ncbi.nlm.nih.gov/dbvar">dbVar</a> is NCBI's database of human genomic structural variation —
     * insertions, deletions, duplications, inversions, mobile elements, translocations, and others.
     * <p>
     * This source represents the individual structural variants (<u>not</u> the SV regions).
     * </p>
     * <p>
     * <a href="https://www.ncbi.nlm.nih.gov/dbvar/content/ftp_manifest/">FTP downloads</a>, data available for both
     * GRCh37 and GRCh38 assemblies in VCF format.
     * </p>
     */
    DBVAR_VARIANT,

    /**
     * Structural variants present in the GoNL database.
     * <p>
     * Downloads are <a href="https://molgenis26.target.rug.nl/downloads/gonl_public/variants/release6.1/">here</a>.
     * </p>
     */
    GONL,

    /**
     * Structural variants from the Abel/Hall data set described <a href="https://www.biorxiv.org/content/10.1101/508515v1">here</a>.
     * <p>
     * <p>
     * Downloads are <a href="https://www.biorxiv.org/content/10.1101/508515v1.supplementary-material">here</a>.
     * </p>
     */
    ABEL,

    /**
     *
     */
    HAPLOINSUFFICIENCY,

    /**
     *
     */
    CLINGEN_HAPLOINSUFFICIENCY,

    /**
     * Data produced by <em>Dosage Sensitivity Curation Working Group</em> of the <em>ClinGen Consortium</em>. The goal
     * of this group is to curate regions of the genome with respect to their dosage sensitivity.<br>
     */
    CLINGEN_TRIPLOSENSITIVITY;

    private static final Logger LOGGER = LoggerFactory.getLogger(PopulationVariantOrigin.class);

    public static Set<PopulationVariantOrigin> benign() {
        return EnumSet.of(GNOMAD_SV, DGV, HGSVC2);
    }

}
