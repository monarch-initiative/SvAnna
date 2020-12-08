package org.jax.svanna.cli;

import org.monarchinitiative.variant.api.Contig;
import org.monarchinitiative.variant.api.GenomicAssembly;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simple genomic assembly implementation good for testing purposes.
 */
class GenomicAssemblyDefault implements GenomicAssembly {

    private final String name;
    private final String organismName;
    private final String taxId;
    private final String submitter;
    private final String date;
    private final String genBankAccession;
    private final String refSeqAccession;
    private final SortedSet<Contig> contigs;
    private final Map<Integer, Contig> contigById;
    private final Map<String, Contig> contigByName;

    GenomicAssemblyDefault(String name,
                           String organismName,
                           String taxId,
                           String submitter,
                           String date,
                           String genBankAccession,
                           String refSeqAccession,
                           Collection<Contig> contigs) {
        this.name = Objects.requireNonNull(name, "Name must not be null");
        this.organismName = Objects.requireNonNull(organismName, "Organism name must not be null");
        this.taxId = Objects.requireNonNull(taxId, "Taxon ID must not be null");
        this.submitter = Objects.requireNonNull(submitter, "Submitter must not be null");
        this.date = Objects.requireNonNull(date, "Date must not be null");
        this.genBankAccession = Objects.requireNonNull(genBankAccession, "Genbank accession must not be null");
        this.refSeqAccession = Objects.requireNonNull(refSeqAccession, "Refseq accession must not be null");
        this.contigs = new TreeSet<>(Objects.requireNonNull(contigs, "Contigs must not be null"));
        if (contigs.isEmpty()) {
            throw new IllegalArgumentException("Contigs must not be empty");
        }
        this.contigById = contigs.stream().collect(Collectors.toMap(Contig::id, Function.identity()));
        this.contigByName = new HashMap<>();
        for (Contig contig : contigs) {
            contigByName.put(contig.name(), contig);
            contigByName.put(contig.genBankAccession(), contig);
            contigByName.put(contig.refSeqAccession(), contig);
            contigByName.put(contig.ucscName(), contig);
        }
    }


    @Override
    public String name() {
        return name;
    }

    @Override
    public String organismName() {
        return organismName;
    }

    @Override
    public String taxId() {
        return taxId;
    }

    @Override
    public String submitter() {
        return submitter;
    }

    @Override
    public String date() {
        return date;
    }

    @Override
    public String genBankAccession() {
        return genBankAccession;
    }

    @Override
    public String refSeqAccession() {
        return refSeqAccession;
    }

    @Override
    public SortedSet<Contig> contigs() {
        return contigs;
    }

    @Override
    public Contig contigById(int contigId) {
        return contigById.get(contigId);
    }

    @Override
    public Contig contigByName(String contigName) {
        return contigByName.get(contigName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenomicAssemblyDefault that = (GenomicAssemblyDefault) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(organismName, that.organismName) &&
                Objects.equals(taxId, that.taxId) &&
                Objects.equals(submitter, that.submitter) &&
                Objects.equals(date, that.date) &&
                Objects.equals(genBankAccession, that.genBankAccession) &&
                Objects.equals(refSeqAccession, that.refSeqAccession) &&
                Objects.equals(contigs, that.contigs) &&
                Objects.equals(contigById, that.contigById) &&
                Objects.equals(contigByName, that.contigByName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, organismName, taxId, submitter, date, genBankAccession, refSeqAccession, contigs, contigById, contigByName);
    }

    @Override
    public String toString() {
        return "GenomicAssemblySimple{" +
                "name='" + name + '\'' +
                ", organismName='" + organismName + '\'' +
                ", taxId='" + taxId + '\'' +
                ", submitter='" + submitter + '\'' +
                ", date='" + date + '\'' +
                ", genBankAccession='" + genBankAccession + '\'' +
                ", refSeqAccession='" + refSeqAccession + '\'' +
                ", contigs=" + contigs +
                ", contigById=" + contigById +
                ", contigByName=" + contigByName +
                '}';
    }
}
