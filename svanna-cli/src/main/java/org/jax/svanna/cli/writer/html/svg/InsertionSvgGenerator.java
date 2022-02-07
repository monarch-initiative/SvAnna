package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.enhancer.Enhancer;
import org.jax.svanna.model.landscape.repeat.RepetitiveRegion;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.sgenes.model.Gene;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class InsertionSvgGenerator extends SvSvgGenerator {


    /**
     * @param variant a structural variant (SV)
     * @param genes gene or genes that overlap with the SV
     * @param enhancers enhancers that overlap with the SV
     * @param repeats repeat regions that overlap with the SV
     * @param dosageRegions triplo/haplosensitive regions that overlap with the SV
     */
    public InsertionSvgGenerator(GenomicVariant variant,
                                 List<Gene> genes,
                                 List<Enhancer> enhancers,
                                 List<RepetitiveRegion> repeats,
                                 List<DosageRegion> dosageRegions) {
        super(variant, genes, enhancers, repeats, dosageRegions);
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
        y = writeDosage(writer, y);
        y = writeRepeats(writer, y);
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
    private void writeInsertion(int xpos, int ypos, String msg, Writer writer) throws IOException {
        double start = translateGenomicToSvg(variant.start());
        int verticalOffset = 20;
        int horizontalOffset = 7;
        String points = String.format("<polygon points=\"%d,%d %d,%d %d,%d\" />",
                xpos, ypos, xpos-horizontalOffset, ypos-verticalOffset, xpos+horizontalOffset, ypos-verticalOffset);
        String rect = String.format("<svg class mytriangle>%s</svg>\n", points);
        writer.write(rect);
        double Y = ypos+30;
        writer.write(String.format("<text x=\"%f\" y=\"%f\"  fill=\"%s\">%s</text>\n", start - 10,Y, PURPLE, msg));
    }


}
