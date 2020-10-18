package org.jax.l2o.vcf;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.data.*;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.progress.ProgressReporter;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import htsjdk.samtools.SAMSequenceDictionary;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;
import org.jax.l2o.except.L2ORuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

import static org.jax.l2o.vcf.SvType.BND;

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

    private List<VcfOverlapList> vcfOverlapListList = new ArrayList<>();
    /**
     * When we are parsing the SVs line for line, we will encounter the first pair of each BND on one line
     * and its pair on a subsequent line. We can recognize this by the MATEID of the first mate, which will
     * be identical with the ID of the second mate.
     */
    private Map<String, BndAnnotation> bndMap;

    /**
     * List of all names in the VCF file
     */
    private List<String> samplenames;

    public VcfSvParser(String vcfpath, String jannovarPath) {
        this.vcfPath = vcfpath;
        this.jannovarData = jannovarData(jannovarPath);
        this.referenceDictionary = this.jannovarData.getRefDict();
        this.chromosomeMap = this.jannovarData.getChromosomes();
        bndMap = new HashMap<>();
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
            final SAMSequenceDictionary seqDict = VCFFileReader.getSequenceDictionary(new File(vcfPath));
            VCFHeader vcfHeader = vcfReader.getFileHeader();
            this.samplenames = vcfHeader.getSampleNamesInOrder();
            this.n_samples = samplenames.size();
            this.samplename = samplenames.get(0);
            logger.trace("Annotating VCF at " + vcfPath + " for sample " + this.samplename);
            CloseableIterator<VariantContext> iter = vcfReader.iterator();
            // The following is from Jannovar -- it is not providing accurate annotations for pbsv
//            VariantContextAnnotator variantEffectAnnotator =
//                    new VariantContextAnnotator(this.referenceDictionary, this.chromosomeMap,
//                            new VariantContextAnnotator.Options());
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


                String contig = vc.getContig();
                if (!this.referenceDictionary.getContigNameToID().containsKey(contig)) {
                    System.err.println("[ERROR] Could not get key for contig :\"" + contig + "\"");
                    continue;
                }
                int id = this.referenceDictionary.getContigNameToID().get(contig);
                Map<String, Object> attributes = vc.getAttributes();
                String svTypeString = (String) attributes.getOrDefault("SVTYPE", "UNKNOWN");
                SvType svtype = SvType.fromString(svTypeString);


                if (svtype.equals(BND)) {
                    String mateId = (String) attributes.getOrDefault("MATEID", "n/a");
                    if (bndMap.containsKey(mateId)) {
                        // add mate information to the BND
                        BndAnnotation firstMate = bndMap.get(mateId);
                        firstMate.addSecondMate(attributes, vc);
                        System.out.println("bnd" + firstMate);
                    } else {
                        String vcfId = vc.getID();
                        BndAnnotation bnd = new BndAnnotation(attributes, vc);
                        // add first mate of a BND annotation.
                        this.bndMap.put(vcfId, bnd);
                        System.out.println("bnd" + bnd);
                    }
                    // Note we build the final annotations for BNDs later on.
                    continue;
                }
                GenomeInterval structVarInterval = new GenomeInterval(referenceDictionary, strand, id, vc.getStart(), vc.getEnd());
                try {
                    // find overlapping transcripts using the interval array of Jannovar
                    IntervalArray<TranscriptModel> iarray = this.chromosomeMap.get(id).getTMIntervalTree();
                    IntervalArray<TranscriptModel>.QueryResult queryResult = iarray.findOverlappingWithInterval(vc.getStart(), vc.getEnd());
                    VcfOverlapList overlap = VcfOverlapList.factory(structVarInterval, queryResult);
                    System.out.println(overlap);
                    if (overlap.isCoding()) {
                        vcfOverlapListList.add(overlap);
                        System.out.println("[VcfOverlap] " + overlap);
                    }
                } catch (L2ORuntimeException e) {
                    e.printStackTrace();
                    System.out.println("[Could not annotate VariantContext] " + vc);
                    System.out.println();
                    throw e;
                } catch (RuntimeException rte) {
                    System.err.printf("[ERROR] Runtime exception for vs start %d vc end %d\n", vc.getStart(), vc.getEnd());
                }
            }
        }
        final long endTime = System.nanoTime();
        final long duration = endTime - startTime;
        System.out.printf("[INFO] VCF input in %.2f seconds.\n", (double)duration/1_000_000_000);
        // Now process the BND
        System.out.printf("[INFO] We identified %d BND structural variants.\n", this.bndMap.size());
        int pair_bnd = 0;
        int single_end = 0;
        for (BndAnnotation band : this.bndMap.values()) {
            if (band.hasMate()) {
                pair_bnd++;
            } else {
                single_end++;
            }
            SvAnn svann = SvAnnFactory.fromBnd(band);
            System.out.println(svann);
            if (svann.getSvType().equals(SvType.UNKNOWN)) {
                int x = 42;
                throw new L2ORuntimeException("Could not find SvType for " + band);
            }
        }
        System.out.printf("[INFO] %d Paired BND structural variants; %d single break end BNDs.\n", pair_bnd, single_end);
    }

    public List<VcfOverlapList> getVcfOverlapListList() {
        return vcfOverlapListList;
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
