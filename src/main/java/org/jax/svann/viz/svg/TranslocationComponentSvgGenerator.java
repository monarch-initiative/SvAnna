package org.jax.svann.viz.svg;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.SvType;

import java.io.IOException;
import java.io.Writer;
import java.util.List;


/**
 * This class is used to create each of the two rows of the final display of the translocation.
 */
public class TranslocationComponentSvgGenerator extends  SvSvgGenerator {


    /**
     * The constructor calculates the left and right boundaries for display
     * TODO document logic, cleanup
     *
     * @param transcripts
     * @param enhancers       // * @param genomeInterval
     * @param coordinatePairs
     */
    public TranslocationComponentSvgGenerator(List<TranscriptModel> transcripts, List<Enhancer> enhancers, List<CoordinatePair> coordinatePairs) {
        super(SvType.TRANSLOCATION, transcripts, enhancers, coordinatePairs);
    }

    @Override
    public void write(Writer writer) throws IOException {

    }
}
