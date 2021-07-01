package org.jax.svanna.cli.writer.html;


import org.monarchinitiative.svart.Contig;

/**
 * A POJO that records the locations of chromosomal breakends or locations in an easily displayable fashion
 */
public class HtmlLocation {

    private final String chrom;
    private final int begin;
    private final int end;

    public HtmlLocation(Contig chrom, int begin, int end) {
        this.chrom = getChromString(chrom);
        this.begin = begin;
        this.end = end;
    }

    public HtmlLocation(Contig chrom, int pos) {
        this.chrom = getChromString(chrom);
        this.begin = pos;
        this.end = pos;
    }

    private String getChromString(Contig chrom) {
        String c = chrom.name();
        if (c.startsWith("chr"))
            // TODO -- this class needs refactoring once we have implemented a few more cases!
            //
            // TODO: 9. 11. 2020 this should actually never happen when using the genome assembly created from the
            //  bundled assembly report files. The primary names are look like `1`, `2`, ..., `22`, `X`, `Y`, `MT`, `HG1032_PATCH`, etc..
            //  Please check the first column of the assembly report files at `src/resources`.
            return  c;
        else
            return "chr" + c;
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
