package org.jax.svann.viz.svg;

import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.genomicreg.Enhancer;
import org.jax.svann.reference.CoordinatePair;
import org.jax.svann.reference.SvType;
import org.jax.svann.reference.transcripts.SvAnnTxModel;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class InsertionSvgGenerator extends SvSvgGenerator {


    private final CoordinatePair insertionCoordinates;

    private final int insertionLen;


    public InsertionSvgGenerator(List<SvAnnTxModel> transcripts,
                                 List<Enhancer> enhancers,
                                 List<CoordinatePair> coordinatePairs,
                                 int insertionLength) {
        super(SvType.INSERTION, transcripts, enhancers, coordinatePairs);
        if (coordinatePairs.size() != 1) {
            throw new SvAnnRuntimeException("Malformed initialization of InsertionSvgGenerator -- we expect one CoordinatePair but got " +
                    coordinatePairs.size());
        }
        insertionCoordinates = coordinatePairs.get(0);
        this.insertionLen = insertionLength;
    }

    public void write(Writer writer) throws IOException {
        int starty = 50;
        int y = starty;
        double start = translateGenomicToSvg(this.insertionCoordinates.getStartPosition());
        double end = translateGenomicToSvg(this.insertionCoordinates.getEndPosition());
        int xpos = (int)(0.5*(start+end));
        String insertionLength = getSequenceLengthString(this.insertionLen);
        String insertionDescription = String.format("%s insertion", insertionLength);
        writeInsertion(xpos, starty, insertionDescription, writer);
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
    private void writeInsertion(int xpos, int ypos, String msg, Writer writer) throws IOException {
        double start = translateGenomicToSvg(this.insertionCoordinates.getStartPosition());
        double end = translateGenomicToSvg(this.insertionCoordinates.getEndPosition());
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
