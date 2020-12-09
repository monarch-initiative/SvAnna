package org.jax.svann.viz;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.TestBase;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.hpo.GeneWithId;
import org.jax.svann.parse.TestVariants.Deletions;
import org.jax.svann.reference.SequenceRearrangement;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO -- Can we make better tests?
 */
public class HtmlVisualizerTest extends TestBase {

    private static final SequenceRearrangement surf1Exon3Deletion = Deletions.surf2singleExon_exon3();
    private static final Map<String, TranscriptModel> transcriptmap = JANNOVAR_DATA.getTmByAccession();
    private static final TranscriptModel fbn1 = transcriptmap.get("NM_000138.4");
    private static final Set<TranscriptModel> affectedTranscripts = Set.of(fbn1);
    private static final GeneWithId fbn1WithId = new GeneWithId("FBN1", TermId.of("NCBIGene:2200"));
    private static final Set<GeneWithId> affectedGeneIds = Set.of(fbn1WithId);
    private static final List<Enhancer> enhancers = List.of(); // no affected enhancers for this
//    private static final SvPriority svpriority =
//            new DefaultSvPriority(surf1Exon3Deletion,
//                    SvType.DELETION,
//                    SvImpact.HIGH_IMPACT,
//                    affectedTranscripts,
//                    affectedGeneIds,
//                    enhancers,
//                    List.of());


//    @Test
//    public void testCtor() {
//        assertNotNull(svpriority);
//    }


    @Test
    public void testGetHtml() {
//        Visualizable visualizable = new HtmlVisualizable(svpriority);
//        Visualizer visualizer = new HtmlVisualizer(visualizable);
//        String html = visualizer.getHtml();
//        System.out.println(html);
    }


}
