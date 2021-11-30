package org.jax.svanna.cli.writer.html.svg;

import org.jax.svanna.core.SvAnnaRuntimeException;
import org.jax.svanna.model.landscape.dosage.DosageRegion;
import org.jax.svanna.model.landscape.dosage.DosageSensitivity;
import org.monarchinitiative.svart.Strand;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static org.jax.svanna.cli.writer.html.svg.Constants.REPEAT_HEIGHT;
import static org.jax.svanna.cli.writer.html.svg.SvSvgGenerator.*;

public class SvgDosageWriter {
    private final List<DosageRegion> dosageRegions;
    private final int paddedGenomicMinPos;
    private final int genomicMinPos;
    private final int genomicMaxPos;
    private final double genomicSpan;



    public SvgDosageWriter(List<DosageRegion> dosages,
                           int paddedGenomicMinimumPos,
                           int paddedGenomicMaximumPos,
                           int genomicMinimumPos,
                           int genomicMaximumPos) {
        this.paddedGenomicMinPos = paddedGenomicMinimumPos;
        this.genomicMinPos = genomicMinimumPos;
        this.genomicMaxPos = genomicMaximumPos;
        this.genomicSpan = paddedGenomicMaximumPos - this.paddedGenomicMinPos;
        this.dosageRegions = dosages;
    }

    /**
     * Our assumption is that each gene is either haploinsensitive, triplosensitive, or neighther
     * Therefore, we need to have only one vertical slot
     * @return the vertical space that will be taken up by the repeat tracks
     */
    public double verticalSpace() {
        return 40d;
    }


    /**
     * @return the SVG x coordinate that corresponds to a given genomic position
     */
    protected double translateGenomicToSvg(int genomicCoordinate) {
        double pos = genomicCoordinate - paddedGenomicMinPos;
        if (pos < 0) {
            String msg = String.format("(repeat writer)Bad left boundary (genomic coordinate: %d) with genomicMinPos=%d and genomicSpan=%.1f pos=%.1f\n",
                    genomicCoordinate, paddedGenomicMinPos, genomicSpan, pos);
            throw new SvAnnaRuntimeException(msg); // should never happen
        }
        double prop = pos / genomicSpan;
        return prop * Constants.SVG_WIDTH;
    }


    private String getDosageColor(DosageSensitivity dose) {
         switch(dose) {
            case NONE: return  LIGHT_GREY;
             case TRIPLOSENSITIVITY: return ORANGE;
             case HAPLOINSUFFICIENCY: return PURPLE;
        }
        // for compiler, can never happen, extended switch is nicer!
        return WHITE;
    }


    public void write(Writer writer, double ystart) throws IOException {
        double minx = translateGenomicToSvg(this.genomicMinPos);
        double maxx = translateGenomicToSvg(this.genomicMaxPos);
        for (var dose : dosageRegions) {
            DosageSensitivity dosageType = dose.isHaploinsufficient() ? DosageSensitivity.HAPLOINSUFFICIENCY :
                    dose.isTriplosensitive() ? DosageSensitivity.TRIPLOSENSITIVITY :
                            DosageSensitivity.NONE;
            if (dosageType.equals(DosageSensitivity.NONE)) continue; // do not show "none" sensitivities
            String sensitivity = dose.isHaploinsufficient() ? "haplosensitive" : "triplosensitive";
            int start = dose.startOnStrand(Strand.POSITIVE);
            int end = dose.endOnStrand(Strand.POSITIVE);
            double x_start_dosage = translateGenomicToSvg(start);
            double x_end_dosage = translateGenomicToSvg(end);
            double repeat_width = x_end_dosage - x_start_dosage;
                String color = getDosageColor(dosageType);
            writer.write(SvgUtil.svgboxThinFrame(x_start_dosage, ystart, repeat_width, REPEAT_HEIGHT, "black", color) + "\n");
            // add label at the top left of the bar
            double ylabel = ystart - REPEAT_HEIGHT/2;
            double xlabel = x_start_dosage + 10;
            writer.write(SvgUtil.svgitalic(xlabel, ylabel, BLACK, sensitivity));
        }
    }
}
