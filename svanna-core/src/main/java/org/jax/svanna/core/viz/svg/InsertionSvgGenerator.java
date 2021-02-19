package org.jax.svanna.core.viz.svg;

import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.reference.Transcript;
import org.monarchinitiative.svart.Variant;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.jax.svanna.core.viz.svg.Constants.HEIGHT_PER_DISPLAY_ITEM;

public class InsertionSvgGenerator extends SvSvgGenerator {


    public InsertionSvgGenerator(Variant variant,
                                 List<Transcript> transcripts,
                                 List<Enhancer> enhancers) {
        super(variant, transcripts, enhancers);
    }

    public void write(Writer writer) throws IOException {
        int starty = 50;
        int y = starty;
        double start = translateGenomicToSvg(variant.start());
        double end = translateGenomicToSvg(variant.end());
        int xpos = (int)(0.5*(start+end));
        String insertionLength = getSequenceLengthString(variant.changeLength());
        String insertionDescription = String.format("%s insertion", insertionLength);
        writeInsertion(xpos, starty, insertionDescription, writer);
        y += 100;
        for (var tmod : affectedTranscripts) {
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
    private void writeInsertion(int xpos, int ypos, String msg, Writer writer) throws IOException {
        double start = translateGenomicToSvg(variant.start());
        double end = translateGenomicToSvg(variant.end());
        double width = end - start;
        double Y = ypos + 0.5 * SV_HEIGHT;
        int verticalOffset = 20;
        int horizontalOffset = 7;
        String points = String.format("<polygon points=\"%d,%d %d,%d %d,%d\" />",
                xpos, ypos, xpos-horizontalOffset, ypos-verticalOffset, xpos+horizontalOffset, ypos-verticalOffset);
        String rect = String.format("<svg class mytriangle>%s</svg>\n",points);
        writer.write(rect);
        Y = ypos+30;
        writer.write(String.format("<text x=\"%f\" y=\"%f\"  fill=\"%s\">%s</text>\n",start -10,Y, PURPLE, msg));
    }


}
