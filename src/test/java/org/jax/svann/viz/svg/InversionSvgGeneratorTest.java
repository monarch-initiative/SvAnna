package org.jax.svann.viz.svg;

import de.charite.compbio.jannovar.data.ReferenceDictionary;
import de.charite.compbio.jannovar.impl.intervals.IntervalArray;
import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.TestBase;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.HpoDiseaseSummary;
import org.jax.svann.overlap.EnhancerOverlapper;
import org.jax.svann.overlap.Overlap;
import org.jax.svann.overlap.Overlapper;
import org.jax.svann.overlap.SvAnnOverlapper;
import org.jax.svann.parse.TestVariants;
import org.jax.svann.priority.PrototypeSvPrioritizer;
import org.jax.svann.priority.SvPrioritizer;
import org.jax.svann.priority.SvPriority;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.SequenceRearrangement;
import org.jax.svann.reference.transcripts.SvAnnTxModel;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class InversionSvgGeneratorTest extends TestBase {

    private static final Collection<TranscriptModel> transcripts = JANNOVAR_DATA.getTmByAccession().values();
    private static final ReferenceDictionary rd = JANNOVAR_DATA.getRefDict();
    private static final List<Enhancer> enhancers = List.of();
    private static final Set<TermId> enhancerRelevantAncestors = Set.of();
    private static final SequenceRearrangement exonicInversion = TestVariants.Inversions.gckExonic();
    private static final Map<Integer, IntervalArray<Enhancer>> enhancerMap = Map.of();
    private static final Set<TermId> patientTerms = Set.of();
    private static final Overlapper overlapper = new SvAnnOverlapper(TX_SERVICE.getChromosomeMap());
    private static final EnhancerOverlapper enhancerOverlapper = new EnhancerOverlapper(enhancerMap);
    private static final Map<TermId, Set<HpoDiseaseSummary>> relevantGenesAndDiseases = Map.of();
    private static final SvPrioritizer prioritizer = new PrototypeSvPrioritizer(overlapper,
            enhancerOverlapper,
            GENE_WITH_ID_MAP,
            patientTerms,
            enhancerRelevantAncestors,
            relevantGenesAndDiseases);
    private static final SvPriority priority = prioritizer.prioritize(exonicInversion);


    @Test
    public void testWriteSvg() {
        List<Overlap> overlaps = overlapper.getOverlapList(exonicInversion);
        List<SvAnnTxModel> transcriptModels = overlaps.stream().map(Overlap::getTranscriptModel).collect(Collectors.toList());
        List<Enhancer> enhancerList = List.of();
        List<CoordinatePair> cpairs = exonicInversion.getRegions();
        SvSvgGenerator gen = new InversionSvgGenerator(transcriptModels, enhancerList, cpairs);
        String svg = gen.getSvg();
        assertNotNull(svg);
        System.out.println(svg);
        try {
            String path = "target/inversion.svg";
            BufferedWriter writer = new BufferedWriter(new FileWriter(path));
            writer.write(svg);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
