package org.jax.svanna.cli.cmd;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.phenopackets.schema.v1.Phenopacket;
import org.phenopackets.schema.v1.core.HtsFile;
import org.phenopackets.schema.v1.core.PhenotypicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This class ingests a phenopacket, which is required to additionally contain the
 * path of a VCF file that will be used for the analysis.
 * @author Peter Robinson
 */
public class PhenopacketImporter {

    private static final Logger logger = LoggerFactory.getLogger(PhenopacketImporter.class);
    private static final JsonFormat.Parser JSON_PARSER = JsonFormat.parser();
    /** The Phenopacket that represents the individual being sequenced in the current run. */
    private final Phenopacket phenoPacket;
    /** Object representing the VCF file with variants identified in the subject of this Phenopacket. */
    private HtsFile vcfFile;
    /** Reference to HPO ontology */
    private final Ontology hpo;

    /**
     * Factory method to obtain a PhenopacketImporter object starting from a phenopacket in Json format
     *
     * @param phenopacketPath -- path to the phenopacket
     * @return {@link PhenopacketImporter} object corresponding to the PhenoPacket
     */
    public static PhenopacketImporter fromJson(Path phenopacketPath, Ontology ontology) throws IOException {
        logger.trace("Importing Phenopacket: " + phenopacketPath);
        if (!phenopacketPath.toFile().isFile()) {
            logger.error("Could not find phenopacket file at " + phenopacketPath);
            throw new IOException("Could not find phenopacket file at " + phenopacketPath);
        }
        try {
            Phenopacket phenopacket = readPhenopacket(phenopacketPath);
            return new PhenopacketImporter(phenopacket, ontology);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Malformed phenopacket: " + e.getMessage());
            throw new IOException("Could not load phenopacket (" + phenopacketPath + "): " + e.getMessage());
        } catch (IOException e) {
            throw new IOException("I/O Error: Could not load phenopacket  (" + phenopacketPath + "): " + e.getMessage(), e);
        }
    }

    public static Phenopacket readPhenopacket(Path phenopacketPath) throws IOException {
        logger.info("Reading phenopacket from `{}`", phenopacketPath.toAbsolutePath());
        try (BufferedReader reader = Files.newBufferedReader(phenopacketPath)) {
            String phenopacketJsonString = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            Phenopacket.Builder phenoPacketBuilder = Phenopacket.newBuilder();
            JSON_PARSER.merge(phenopacketJsonString, phenoPacketBuilder);
            return phenoPacketBuilder.build();
        }
    }

    private PhenopacketImporter(Phenopacket ppack, Ontology ontology){
        this.phenoPacket=ppack;
        this.hpo=ontology;
        extractVcfData();
    }

    public boolean hasVcf() { return  this.vcfFile !=null; }

    public List<TermId> getHpoTerms() {
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (PhenotypicFeature feature : phenoPacket.getPhenotypicFeaturesList()) {
            if (feature.getNegated()) continue;
            String id = feature.getType().getId();
            TermId tid = TermId.of(id);
            if (! hpo.getTermMap().containsKey(tid)) {
                logger.error("Could not identify HPO term id {}.",tid.getValue());
                logger.error("Please check the input file and update to the latest hp.obo file. ");
                throw new PhenolRuntimeException("Could not identify HPO term id: "+tid.getValue());
            } else if (hpo.getObsoleteTermIds().contains(tid)) {
                TermId current =  hpo.getPrimaryTermId(tid);
                builder.add(current);
                logger.error("Replacing obsolete HPO term id {} with current id {}.",tid.getValue(),current.getValue());
            } else {
                builder.add(tid);
            }
        }
        return builder.build();
    }


    public List<TermId> getNegatedHpoTerms() {
        ImmutableList.Builder<TermId> builder = new ImmutableList.Builder<>();
        for (PhenotypicFeature feature : phenoPacket.getPhenotypicFeaturesList()) {
            if (! feature.getNegated()) continue;
            String id = feature.getType().getId();
            TermId tid = TermId.of(id);
            if (! hpo.getTermMap().containsKey(tid)) {
                logger.error("Could not identify HPO term id {}.",tid.getValue());
                logger.error("Please check the input file and update to the latest hp.obo file. ");
                throw new PhenolRuntimeException("Could not identify HPO term id: "+tid.getValue());
            } else if (hpo.getObsoleteTermIds().contains(tid)) {
                TermId current =  hpo.getPrimaryTermId(tid);
                builder.add(current);
                logger.error("Replacing obsolete HPO term id {} with current id {}.", tid.getValue(), current.getValue());
            } else {
                builder.add(tid);
            }
        }
        return builder.build();
    }

    /**
     * The path to the VCF file may be a string such as file:/path/to/examples/BBS1.vcf
     * In this case, remove the prefix 'path:', otherwise return the original URI
     * @return URI of VCF file mentioned in the Phenopacket
     */


    public HtsFile getVcfFile() {
        return this.vcfFile;
    }

    public Path getVcfPath() {
        if (this.vcfFile == null) {
            return null;
        }
        String uri = this.vcfFile.getUri().startsWith("file:") ?
                this.vcfFile.getUri().substring(5) :
                this.vcfFile.getUri();
        return Paths.get(uri);
    }

    public String getSampleName() {
        return phenoPacket.getSubject().getId();
    }


    /** This method extracts the VCF file and the corresponding GenomeBuild. We assume that
     * the phenopacket contains a single VCF file and that this file is for a single person. */
    private void extractVcfData() {
        List<HtsFile> htsFileList = phenoPacket.getHtsFilesList();
        if (htsFileList.size() > 1 ) {
            logger.error("Warning: multiple HTsFiles associated with this phenopacket");
            logger.error("Warning: we will return the path to the first VCF file we find");
        } else if (htsFileList.isEmpty()) {
            return;
        }
        for (HtsFile htsFile : htsFileList) {
            if (htsFile.getHtsFormat().equals(HtsFile.HtsFormat.VCF)) {
                this.vcfFile = htsFile;
            }
        }
    }
}
