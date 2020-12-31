package org.jax.svann.parse;

import de.charite.compbio.jannovar.data.JannovarData;
import de.charite.compbio.jannovar.data.JannovarDataSerializer;
import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.data.SerializationException;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.overlap.SvAnnOverlapper;
import org.jax.svann.priority.PrototypeSvPrioritizer;
import org.jax.svann.priority.SvPrioritizer;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.Strand;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.jax.svann.reference.transcripts.JannovarTranscriptService;
import org.jax.svann.reference.transcripts.SvAnnTxModel;
import org.jax.svann.reference.transcripts.TranscriptService;
import org.jax.svann.viz.svg.DeletionSvgGenerator;
import org.jax.svann.viz.svg.SvSvgGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * I am using this to create splicing graphics. We do not need this to test the app and the class can be deleted before
 * publication TODO
 */
@Disabled
public class DeletionSvgGeneratorTest {
    private static GenomeAssembly GENOME_ASSEMBLY;
    private static SvPrioritizer prioritizer;
    private static Overlapper overlapper;

    private static JannovarData readJannovarData(String jannovarPath) throws SerializationException {
        return new JannovarDataSerializer(jannovarPath).load();
    }

    protected static final Map<String, GeneWithId> GENE_WITH_ID_MAP = Map.of();

    @BeforeAll
    public static void init() throws Exception {
        final String jannovarPath = "/home/peter/GIT/jannovar/data/hg38_ensembl.ser";
        JannovarData JANNOVAR_DATA = readJannovarData(jannovarPath);
        Optional<GenomeAssembly> assemblyOptional = GenomeAssemblyProvider.getDefaultProvider().getAssembly("GRCh38.p13");
        if (assemblyOptional.isEmpty()) {
            throw new RuntimeException("Assembly GRCh38.p13 not available");
        }
        GENOME_ASSEMBLY = assemblyOptional.get();
        TranscriptService transcriptService = JannovarTranscriptService.of(GENOME_ASSEMBLY, JANNOVAR_DATA);
        final Collection<TranscriptModel> transcripts = JANNOVAR_DATA.getTmByAccession().values();
        final ReferenceDictionary rd = JANNOVAR_DATA.getRefDict();
        final List<Enhancer> enhancers = List.of();
        final Set<TermId> enhancerRelevantAncestors = Set.of();
        final SequenceRearrangement exonicInversion = TestVariants.Inversions.gckExonic();
        final Map<Integer, IntervalArray<Enhancer>> enhancerMap = Map.of();
        final Set<TermId> patientTerms = Set.of();
        overlapper = new SvAnnOverlapper(transcriptService.getChromosomeMap());
        final EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(enhancerMap);
        final Map<TermId, Set<HpoDiseaseSummary>> relevantGenesAndDiseases = Map.of();
        prioritizer = new PrototypeSvPrioritizer(overlapper,
                enhancerOverlapper,
                GENE_WITH_ID_MAP,
                patientTerms,
                enhancerRelevantAncestors,
                relevantGenesAndDiseases);
    }



    private void outputSvg(String svg, String title) {
        System.out.println(svg);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(title));
            writer.write(svg);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    /**
     *  ANO1 ENSG00000131620
     *  Location
     *
     * Chromosome 11: 70,078,302-70,189,528 forward strand.
     */
    @Test
    public void testANO1() {
        Contig chr11 = GENOME_ASSEMBLY.getContigByName("11").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr11, 70_078_300, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr11, 70_189_000, Strand.FWD, "gck_del_upstream_intergenic_r", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/ANO1.svg");
    }

    /*
    Chromosome 15: 89,830,599-89,894,638 reverse strand.
     */
    @Test
    public void testAP3S2() {
        Contig chr15 = GENOME_ASSEMBLY.getContigByName("15").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr15, 89_830_599, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr15, 89_894_638, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/AP3S2.svg");
    }

    /*
    Chromosome X 41339038        exonEnd:41339083
     */
    @Test
    public void testDDX3X() {
        Contig chrX = GENOME_ASSEMBLY.getContigByName("X").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chrX, 41339038, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chrX, 41339083, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/DDX3X.svg");
    }


    /**
     * chr16:68232370-68232503  ESRP2
     */
    @Test
    public void testESRP2() {
        Contig chr16 = GENOME_ASSEMBLY.getContigByName("16").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr16, 68232370, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr16, 68232503, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/ESRP2.svg");
    }


    /**
     * ESYT2  chr7:158752780-158752843
     */
    @Test
    public void testESYT2() {
        Contig chr7 = GENOME_ASSEMBLY.getContigByName("7").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr7, 158742780, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr7, 158752843, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/ESYT2.svg");
    }

    /**
     * chr10:121518681-121518829  first of two fgfr2 (event 28971)
     */
    @Test
    public void testFGFR2a() {
        Contig chr10 = GENOME_ASSEMBLY.getContigByName("10").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr10, 121518681, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr10, 121518829, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/FGFR2_28971.svg");
    }

    /**
     *  chr10:121517318-121517463 2 of two fgfr2 (event 28972)
     */
    @Test
    public void testFGFR2b() {
        Contig chr10 = GENOME_ASSEMBLY.getContigByName("10").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr10, 121517318, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr10, 121517463, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/FGFR2_28972.svg");
    }

    /**
     * ITGB4 chr17:75755054-75755213
     */
    @Test
    public void testITGB4() {
        Contig chr17 = GENOME_ASSEMBLY.getContigByName("17").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr17, 75755054, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr17, 75755213, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/ITGB4.svg");
    }

    /**
     * chr8:143938400-143938415  PLEC
     */
    @Test
    public void testPLEC() {
        Contig chr8 = GENOME_ASSEMBLY.getContigByName("8").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr8, 143938400, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr8, 143938415, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/PLEC.svg");
    }


    /**
     * chr19:50224153-50224177  MYH14
     */
    @Test
    public void testMYH14() {
        Contig chr19 = GENOME_ASSEMBLY.getContigByName("19").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr19, 50224153, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr19, 50224177, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/MYH14.svg");
    }

    /**
     * SLK chr10:104010815-104010908
     */
    @Test
    public void testSLK() {
        Contig chr10 = GENOME_ASSEMBLY.getContigByName("10").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr10, 104010815, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr10, 104010908, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/SLK.svg");
    }

    /**
     * chr9:128790734-128790775  TBC1D13
     */
    @Test
    public void testTBC1D13() {
        Contig chr9 = GENOME_ASSEMBLY.getContigByName("9").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chr9, 128790734, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chr9, 128790775, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/TBC1D13.svg");
    }


    /**
     * chrX:53218275-53218398  KDM5C
     */
    @Test
    public void testKDM5C() {
        Contig chrX = GENOME_ASSEMBLY.getContigByName("X").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chrX, 53218275, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chrX, 53218398, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/KDM5C.svg");
    }

    /**
     * chrX:53221687-53221730  KDM5C-22850
     */
    @Test
    public void testKDM5Cb() {
        Contig chrX = GENOME_ASSEMBLY.getContigByName("X").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chrX, 53221687, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chrX, 53221730, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/KDM5C_22850.svg");
    }

    /**
     * XIST chrX:73_826_115-73_827_984
     */
    @Test
    public void testXIST() {
        Contig chrX = GENOME_ASSEMBLY.getContigByName("X").orElseThrow();
        BreakendDefault left = BreakendDefault.preciseWithRef(chrX, 73_810_115, Strand.FWD, "upstream", "t");
        BreakendDefault right = BreakendDefault.preciseWithRef(chrX, 73_840_984, Strand.FWD, "down", "t");
        SequenceRearrangement rearrangement = SequenceRearrangementDefault.of(SvType.DELETION, AdjacencyDefault.empty(left, right));
        List<Overlap> overlaps = overlapper.getOverlapList(rearrangement);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = rearrangement.getRegions();
        SvSvgGenerator gen = new DeletionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        outputSvg(svg, "target/XIST.svg");
    }


}
