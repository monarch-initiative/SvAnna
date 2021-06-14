package org.jax.svanna.cli.writer.html.svg;


import org.jax.svanna.core.exception.SvAnnRuntimeException;
import org.jax.svanna.core.landscape.RepetitiveRegion;
import org.monarchinitiative.svart.Strand;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import static org.jax.svanna.cli.writer.html.svg.Constants.REPEAT_HEIGHT;

/**
 * Write a UCSC-style track for each of the repeat families in our SVG. Write each repeat as a region in the
 * corresponding track.
 */
public class SvgRepeatWriter {
    private final Map<String, List<RepetitiveRegion>> repeatFamilyMap;
    private final int paddedGenomicMinPos;
    private final int paddedGenomicMaxPos;
    private final int genomicMinPos;
    private final int genomicMaxPos;
    private final double genomicSpan;



    public SvgRepeatWriter(List<RepetitiveRegion> repeats,
                           int paddedGenomicMinimumPos,
                           int paddedGenomicMaximumPos,
                           int genomicMinimumPos,
                           int genomicMaximumPos) {
        this.paddedGenomicMinPos = paddedGenomicMinimumPos;
        this.paddedGenomicMaxPos = paddedGenomicMaximumPos;
        this.genomicMinPos = genomicMinimumPos;
        this.genomicMaxPos = genomicMaximumPos;
        this.genomicSpan = this.paddedGenomicMaxPos - this.paddedGenomicMinPos;
        repeatFamilyMap = new TreeMap<>();
        for (var repeat : repeats) {
            repeatFamilyMap.putIfAbsent(repeat.repeatFamily().name(), new ArrayList<>());
            repeatFamilyMap.get(repeat.repeatFamily().name()).add(repeat);
        }
    }

    /**
     * @return the vertical space that will be taken up by the repeat tracks
     */
    public double verticalSpace() {
        int numberOfTracks = this.repeatFamilyMap.size();
        return REPEAT_HEIGHT * numberOfTracks + 20;
    }


    /**
     * @return the SVG x coordinate that corresponds to a given genomic position
     */
    protected double translateGenomicToSvg(int genomicCoordinate) {
        double pos = genomicCoordinate - paddedGenomicMinPos;
        if (pos < 0) {
            String msg = String.format("(repeat writer)Bad left boundary (genomic coordinate: %d) with genomicMinPos=%d and genomicSpan=%.1f pos=%.1f\n",
                    genomicCoordinate, paddedGenomicMinPos, genomicSpan, pos);
            throw new SvAnnRuntimeException(msg); // should never happen
        }
        double prop = pos / genomicSpan;
        return prop * Constants.SVG_WIDTH;
    }



    public void write(Writer writer, double ystart) throws IOException {
        double y = ystart;
        double minx = translateGenomicToSvg(this.genomicMinPos);
        double maxx = translateGenomicToSvg(this.genomicMaxPos);
        double trackwidth = maxx-minx;
        for (var e : this.repeatFamilyMap.entrySet()) {
            String family = e.getKey();
            writer.write(SvgUtil.svgbox(minx, y, trackwidth, REPEAT_HEIGHT, SvSvgGenerator.DarkGrey) + "\n");
            writer.write(SvgUtil.svgtext(minx + trackwidth + 10, y, SvSvgGenerator.BLACK, family, 12, 12) + "\n");
            List<RepetitiveRegion> repeatList = e.getValue();
            for (var repeat : repeatList) {
                int start = repeat.startOnStrand(Strand.POSITIVE);
                double x_repeat = translateGenomicToSvg(start);
                int end = repeat.endOnStrand(Strand.POSITIVE);
                double x_end_repeat = translateGenomicToSvg(end);
                double repeat_width = x_end_repeat - x_repeat;
                String repeatColor = Constants.repeatToColor(family);
                writer.write(SvgUtil.svgbox(x_repeat, y, repeat_width, REPEAT_HEIGHT, "black", repeatColor) + "\n");
            }
            y += REPEAT_HEIGHT;
        }
    }
}
