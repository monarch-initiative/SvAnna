package org.jax.svann.viz;

import org.jax.svann.reference.genome.Contig;

/**
 * A POJO that records the locations of chromosomal breakends or locations in an easily displayable fashion
 */
public class HtmlLocation {

    private final String chrom;
    private final int begin;
    private final int end;

    public HtmlLocation(Contig chrom, int begin, int end) {
        String c = chrom.getPrimaryName();
        if (c.startsWith("chr"))
            // TODO: 9. 11. 2020 this should actually never happen when using the genome assembly created from the
            //  bundled assembly report files. The primary names are look like `1`, `2`, ..., `22`, `X`, `Y`, `MT`, `HG1032_PATCH`, etc..
            //  Please check the first column of the assembly report files at `src/resources`.
            this.chrom = c;
        else
            this.chrom = "chr" + c;
        this.begin = begin;
        this.end = end;
    }

    public String getChrom() {
        return chrom;
    }

    public int getBegin() {
        return begin;
    }

    public int getEnd() {
        return end;
    }
}
