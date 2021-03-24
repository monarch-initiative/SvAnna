package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.core.landscape.Enhancer;
import org.jax.svanna.core.reference.Gene;
import org.monarchinitiative.svart.Variant;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class DeletionSvgGenerator extends SvSvgGenerator {

    public DeletionSvgGenerator(Variant variant,
                                List<Gene> genes,
                                List<Enhancer> enhancers) {
        super(variant, genes, enhancers);
    }


    /**
     * Write an SVG (without header) representing this SV. Not intended to be used to create a stand-alone
     * SVG (for this, user {@link #getSvg()}
     * @param writer a file handle
     * @throws IOException if we cannot write.
     */
    public void write(Writer writer) throws IOException {
        int starty = 50;
        int y = starty;
        String deletionLength = getSequenceLengthString(-variant.changeLength()); // negated change length
        String deletionDescription = String.format("%s deletion", deletionLength);
        writeDeletion(starty, deletionDescription, writer);
        y += 100;
        for (var e : affectedEnhancers) {
            writeEnhancer(e, y, writer);
            y += Constants.HEIGHT_PER_DISPLAY_ITEM;
        }
        for (var gene : affectedGenes) {
            writeGene(gene, y, writer);
            y += Constants.HEIGHT_PER_DISPLAY_ITEM;
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
        double start = translateGenomicToSvg(variant.start());
        double end = translateGenomicToSvg(variant.end());
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
