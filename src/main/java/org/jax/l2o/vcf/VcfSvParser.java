package org.jax.l2o.vcf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import de.charite.compbio.jannovar.annotation.VariantAnnotations;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.data.*;
import de.charite.compbio.jannovar.htsjdk.InvalidCoordinatesException;
import de.charite.compbio.jannovar.htsjdk.VariantContextAnnotator;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.progress.ProgressReporter;
import de.charite.compbio.jannovar.reference.GenomePosition;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.commons.io.FilenameUtils;
import org.jax.l2o.except.L2ORuntimeException;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * This class implements parsing structural variants into SvAnnotation objects.
 * The VCF standard describes two types of SV notations. One is by SV types, i.e. insertions, deletions, inversions,
 * translocations, etc. The other is by breakend notations, often labelled with SVTYPE=BND.
 *
 * @author Peter Robinson
 */
public class VcfSvParser {
    private static final Logger logger = LoggerFactory.getLogger(VcfSvParser.class);
    /**
     * The strand is irrelevant for our SV analysis, but it is needed for the Jannovar API.
     */
    private static Strand strand = Strand.FWD;
    /**
     * Path to the VCF file with the exome/genome of the proband.
     */
    private final String vcfPath;
    /**
     * Reference to the Jannovar transcript file data for annotating the VCF file.
     */
    private final JannovarData jannovarData;
    /**
     * Reference dictionary that is part of {@link #jannovarData}.
     */
    private final ReferenceDictionary referenceDictionary;
    /**
     * Map of Chromosomes, used in the annotation.
     */
    private final ImmutableMap<Integer, Chromosome> chromosomeMap;
    /**
     * A Jannovar object to report progress of VCF parsing.
     */
    private ProgressReporter progressReporter = null;
    /**
     * Should be hg37 or hg38
     */
    //private final GenomeAssembly genomeAssembly;
    /**
     * Number of variants that were not filtered.
     */
    private int n_good_quality_variants = 0;
    /**
     * Number of variants that were removed because of the quality filter.
     */
    private int n_filtered_variants = 0;
    /**
     * Number of samples in the VCF file.
     */
    private int n_samples;
    /**
     * Name of the proband in the VCF file.
     */
    private String samplename;
    /**
     * List of all names in the VCF file
     */
    private List<String> samplenames;

    public VcfSvParser(String vcfpath, String jannovarPath) {
        this.vcfPath = vcfpath;
        this.jannovarData = jannovarData(jannovarPath);
        this.referenceDictionary = this.jannovarData.getRefDict();
        this.chromosomeMap = this.jannovarData.getChromosomes();
        parseSvVcf();
    }


    /**
     * Deserialize the Jannovar transcript data file that comes with Exomiser. Note that Exomiser
     * uses its own ProtoBuf serializetion and so we need to use its Deserializser. In case the user
     * provides a standard Jannovar serialzied file, we try the legacy deserializer if the protobuf
     * deserializer doesn't work.
     *
     * @return the object created by deserializing a Jannovar file.
     */
    public JannovarData jannovarData(String jannovarPath) {
        if (jannovarData != null) return jannovarData;
        File f = new File(jannovarPath);
        if (!f.exists()) {
            throw new L2ORuntimeException("[FATAL] Could not find Jannovar transcript file at " + jannovarPath);
        }
        try {
            return new JannovarDataSerializer(jannovarPath).load();
        } catch (SerializationException e) {
            logger.error("Could not deserialize Jannovar file with legacy deserializer...");
            throw new L2ORuntimeException(String.format("Could not load Jannovar data from %s (%s)",
                    jannovarPath, e.getMessage()));
        }
    }


    private void parseSvVcf() {
        // whether or not to just look at a specific genomic interval
        final boolean useInterval = false;

        final long startTime = System.nanoTime();

        try (VCFFileReader vcfReader = new VCFFileReader(new File(vcfPath), useInterval)) {
            //final SAMSequenceDictionary seqDict = VCFFileReader.getSequenceDictionary(new File(getOptionalVcfPath));
            VCFHeader vcfHeader = vcfReader.getFileHeader();
            this.samplenames = vcfHeader.getSampleNamesInOrder();
            this.n_samples = samplenames.size();
            this.samplename = samplenames.get(0);
            logger.trace("Annotating VCF at " + vcfPath + " for sample " + this.samplename);
            CloseableIterator<VariantContext> iter = vcfReader.iterator();
            VariantContextAnnotator variantEffectAnnotator =
                    new VariantContextAnnotator(this.referenceDictionary, this.chromosomeMap,
                            new VariantContextAnnotator.Options());
            int c = 0;
            while (iter.hasNext()) {
                VariantContext vc = iter.next();
                if (vc.isFiltered()) {
                    // this is a failing VariantContext
                    n_filtered_variants++;
                    continue;
                } else {
                    n_good_quality_variants++;
                }

                System.out.println("[VariantContext] " + vc);
                String contig = vc.getContig();
                if (!this.referenceDictionary.getContigNameToID().containsKey(contig)) {
                    System.err.println("[ERR] Could not get key for contig :\"" + contig + "\"");
                    continue;
                }
                int id = this.referenceDictionary.getContigNameToID().get(contig);
                GenomePosition gposStart = new GenomePosition(referenceDictionary, strand, id, vc.getStart());
                GenomePosition gposEnd = new GenomePosition(referenceDictionary, strand, id, vc.getEnd());
                IntervalArray<TranscriptModel> iarray = this.chromosomeMap.get(id).getTMIntervalTree();
                IntervalArray<TranscriptModel>.QueryResult queryResult = iarray.findOverlappingWithInterval(vc.getStart(), vc.getEnd());
                VcfOverlap overlap = VcfOverlap.factory(gposStart, gposEnd, queryResult);
                System.out.println("[VcfOverlap] " + overlap);


                if (++c > 10) break;
            }
        }
    }


    public int getN_samples() {
        return n_samples;
    }

    public String getSamplename() {
        return samplename;
    }

    public List<String> getSamplenames() {
        return samplenames;
    }


    public int getN_good_quality_variants() {
        return n_good_quality_variants;
    }

    public int getN_filtered_variants() {
        return n_filtered_variants;
    }

}
