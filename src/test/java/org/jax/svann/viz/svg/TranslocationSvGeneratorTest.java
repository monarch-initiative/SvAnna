package org.jax.svann.viz.svg;

/**
 *  /**
 *      * Translocation where one CDS is disrupted and the other is not
 *      * <p>
 *      * left mate, SURF2:NM_017503.5 intron 3 (disrupted CDS)
 *      * chr9:133_359_000 (+)
 *      * right mate, upstream from BRCA2 (not disrupted)
 *      * chr13:32_300_000 (+)
 *      */

import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.GenomeInterval;
import de.charite.compbio.jannovar.reference.PositionType;
import de.charite.compbio.jannovar.reference.Strand;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.TestBase;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.genomicreg.TestCoordinatePair;
import org.jax.svann.genomicreg.TestGenomicPosition;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.priority.PrototypeSvPrioritizer;
import org.jax.svann.priority.SvPrioritizer;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.viz.HtmlVisualizable;
import org.jax.svann.viz.HtmlVisualizer;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.jax.svann.parse.TestVariants.translocationWhereOneCdsIsDisruptedAndTheOtherIsNot;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class TranslocationSvGeneratorTest extends TestBase{
    private static final Collection<TranscriptModel> transcripts = JANNOVAR_DATA.getTmByAccession().values();
    private static final ReferenceDictionary rd = JANNOVAR_DATA.getRefDict();
    private static final List<Enhancer> enhancers = List.of();
    private static final Set<TermId> enhancerRelevantAncestors = Set.of();
    private static final SequenceRearrangement translocation = translocationWhereOneCdsIsDisruptedAndTheOtherIsNot();
    private static final Map<Integer, IntervalArray<Enhancer>> enhancerMap = Map.of();
    private static final Set<TermId> patientTerms = Set.of();
    private static final Overlapper overlapper = new Overlapper(JANNOVAR_DATA);
    private static final EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(JANNOVAR_DATA, enhancerMap);
    private static final Map<TermId, Set<HpoDiseaseSummary>> relevantGenesAndDiseases = Map.of();
    private static final SvPrioritizer prioritizer = new PrototypeSvPrioritizer(overlapper,
            enhancerOverlapper,
            GENE_WITH_ID_MAP,
            patientTerms,
            enhancerRelevantAncestors,
            relevantGenesAndDiseases);
    private static final SvPriority priority = prioritizer.prioritize(translocation);
    private static final HtmlVisualizable visualizable = new HtmlVisualizable(translocation, priority);
    private static final HtmlVisualizer visualizer = new HtmlVisualizer();

    @Test
    public void testCreateNonNullSvg() {
        String svg = visualizer.getHtml(visualizable);
        assertNotNull(svg);
    }

    @Test
    public void testWriteSvg() {
       // String svg = visualizer.getHtml(visualizable);
        List<Overlap> overlaps = overlapper.getOverlapList(translocation);
        List<TranscriptModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = translocation.getRegions();
        SvSvgGenerator gen = new TranslocationSvgGenerator(translocation,transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        System.out.println(svg);
        try {
            String path = "target/translocation.svg";
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(svg);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
