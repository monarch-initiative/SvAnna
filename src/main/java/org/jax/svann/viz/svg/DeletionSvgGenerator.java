package org.jax.svann.viz.svg;

import de.charite.compbio.jannovar.reference.TranscriptModel;
import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.SvType;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class DeletionSvgGenerator extends SvSvgGenerator {


    private final CoordinatePair deletionCoordinates;

    public DeletionSvgGenerator(List<TranscriptModel> transcripts,
                                List<Enhancer> enhancers,
                                List<CoordinatePair> coordinatePairs) {
       super(SvType.DELETION, transcripts, enhancers, coordinatePairs);
       if (coordinatePairs.size() != 1) {
           throw new SvAnnRuntimeException("Malformed initialization of DeletionSvgGenerator -- we expect one CoordinatePair but got " +
                   coordinatePairs.size());
       }
        deletionCoordinates = coordinatePairs.get(0);

    }


    /**
     * Wirte an SVG (without header) representing this SV. Not intended to be used to create a stand-alone
     * SVG (for this, user {@link #getSvg()}
     * @param writer a file handle
     * @throws IOException if we cannot write.
     */
    public void write(Writer writer) throws IOException {
        int starty = 50;
        int y = starty;
        String deletionLength = getSequenceLengthString(deletionCoordinates.getLength());
        String deletionDescription = String.format("%s deletion", deletionLength);
        writeDeletion(starty, deletionDescription, writer);
        y += 100;
        for (var tmod : this.affectedTranscripts) {
            writeTranscript(tmod, y, writer);
            y += HEIGHT_PER_DISPLAY_ITEM;
        }
        writeScale(writer, y);
    }

    /**
     * PROTOTYPE -- THIS MAYBE NOT BE THE BEST WAY TO REPRESENT OTHER TUPES OF SV
     * @param ypos  The y position where we will write the cartoon
     * @param msg A String describing the SV
     * @param writer a file handle
     * @throws IOException if we can't write
     */
    private void writeDeletion(int ypos, String msg, Writer writer) throws IOException {
        double start = translateGenomicToSvg(this.deletionCoordinates.getStartPosition());
        double end = translateGenomicToSvg(this.deletionCoordinates.getEndPosition());
        double width = end - start;
        double Y = ypos + 0.5 * SV_HEIGHT;
        String rect = String.format("<rect x=\"%f\" y=\"%f\" width=\"%f\" height=\"%f\" rx=\"2\" " +
                        "style=\"stroke:%s; fill: %s\" />\n",
               start, Y, width, SV_HEIGHT, DARKGREEN, RED);
        writer.write(rect);
        Y += 1.75*SV_HEIGHT;
        writer.write(String.format("<text x=\"%f\" y=\"%f\"  fill=\"%s\">%s</text>\n",start -10,Y, PURPLE, msg));
    }



}
