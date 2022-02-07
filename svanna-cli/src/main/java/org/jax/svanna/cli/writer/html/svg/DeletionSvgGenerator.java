package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.repeat.RepetitiveRegion;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.sgenes.model.Gene;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class DeletionSvgGenerator extends SvSvgGenerator {


    /**
     * @param variant a structural variant (SV)
     * @param genes gene or genes that overlap with the SV
     * @param enhancers enhancers that overlap with the SV
     * @param repeats repeat regions that overlap with the SV
     * @param dosageRegions triplo/haplosensitive regions that overlap with the SV
     */
    public DeletionSvgGenerator(GenomicVariant variant,
                                List<Gene> genes,
                                List<Enhancer> enhancers,
                                List<RepetitiveRegion> repeats,
                                List<DosageRegion> dosageRegions) {
        super(variant, genes, enhancers, repeats, dosageRegions);
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
        y = writeDosage(writer, y);
        y = writeRepeats(writer, y);
        for (var e : affectedEnhancers) {
            writeEnhancer(e, y, writer);
            y += Constants.HEIGHT_PER_DISPLAY_ITEM;
        }
        for (var gene : affectedGenes) {
            writeGene(gene, y, writer);
            y += gene.transcriptCount() * Constants.HEIGHT_PER_DISPLAY_ITEM;
        }
        writeScale(writer, y);
    }

    /**
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
