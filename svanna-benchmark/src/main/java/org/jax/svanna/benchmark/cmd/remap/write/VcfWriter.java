package org.jax.svanna.benchmark.cmd.remap.write;

import htsjdk.variant.variantcontext.*;
import htsjdk.variant.variantcontext.writer.Options;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.*;
import org.jax.svanna.core.reference.Zygosity;
import org.jax.svanna.io.FullSvannaVariant;
import org.monarchinitiative.svart.BreakendVariant;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.util.Seq;
import org.monarchinitiative.svart.util.VcfBreakendFormatter;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class VcfWriter implements FullSvannaVariantWriter {

    private final String sampleName;
    private final Path output;

    public VcfWriter(String sampleName, Path output) {
        this.sampleName = sampleName;
        this.output = output;
    }

    @Override
    public int write(Iterable<FullSvannaVariant> variants) {
        VCFHeader header = createVcfHeader(sampleName);

        try (VariantContextWriter writer = new VariantContextWriterBuilder()
                .unsetOption(Options.INDEX_ON_THE_FLY)
                .setOutputPath(output)
                .build()) {
            writer.writeHeader(header);
            int writtenLines = 0;
            for (FullSvannaVariant variant : variants) {
                VariantContextBuilder vcb = new VariantContextBuilder(variant.variantContext())
                        .chr(variant.contigName())
                        .start(variant.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased()))
                        .stop(variant.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased()));

                if (variant instanceof BreakendVariant) {
                    BreakendVariant bv = (BreakendVariant) variant;
                    String alt = VcfBreakendFormatter.makeAltVcfField(bv);
                    String ref = bv.strand().isPositive() ? bv.ref() : Seq.reverseComplement(bv.ref());

                    setGenotypes(vcb, ref, alt, variant.zygosity());
                } else {
                    vcb.attribute("END", variant.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased()))
                            .attribute("SVLEN", variant.changeLength());
                }

                VariantContext vc = vcb.make();
                writer.add(vc);
                writtenLines++;
            }
            return writtenLines;
        }
    }

    private void setGenotypes(VariantContextBuilder vcb,
                              String ref,
                              String alt,
                              Zygosity zygosity) {
        // variant alleles - REF & ALT VCF fields
        Allele refAllele = Allele.create(ref, true);
        Allele altAllele = Allele.create(alt);
        vcb.alleles(List.of(refAllele, altAllele));

        // sample alleles - SAMPLE field
        Genotype genotype;
        switch (zygosity) {
            case HOMOZYGOUS:
                genotype = GenotypeBuilder.create(sampleName, List.of(altAllele, altAllele));
                break;
            case HEMIZYGOUS:
                genotype = GenotypeBuilder.create(sampleName, List.of(altAllele));
                break;
            default:
            case HETEROZYGOUS:
                genotype = GenotypeBuilder.create(sampleName, List.of(refAllele, altAllele));
                break;
        }
        vcb.genotypes(GenotypesContext.create(genotype));
    }

    private static VCFHeader createVcfHeader(String sampleName) {
        VCFHeader header = new VCFHeader(Set.of(), List.of(sampleName));
        header.setVCFHeaderVersion(VCFHeaderVersion.VCF4_2);

        header.addMetaDataLine(new VCFInfoHeaderLine("SVTYPE", 1, VCFHeaderLineType.String, "Type of structural variant"));
        header.addMetaDataLine(new VCFInfoHeaderLine("SVLEN", VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.Integer, "Difference in length between REF and ALT alleles"));
        header.addMetaDataLine(new VCFInfoHeaderLine("SVANN", VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.String, "Repeat annotation of structural variant"));
        header.addMetaDataLine(new VCFInfoHeaderLine("END", 1, VCFHeaderLineType.Integer, "End position of the structural variant described in this record"));
        header.addMetaDataLine(new VCFInfoHeaderLine("CIPOS", 1, VCFHeaderLineType.Integer, "Confidence interval around POS for imprecise variants"));
        header.addMetaDataLine(new VCFInfoHeaderLine("CIEND", 1, VCFHeaderLineType.Integer, "Confidence interval around END for imprecise variants"));
        header.addMetaDataLine(new VCFInfoHeaderLine("IMPRECISE", 0, VCFHeaderLineType.Flag, "Imprecise structural variation"));
        header.addMetaDataLine(new VCFInfoHeaderLine("SHADOWED", 0, VCFHeaderLineType.Flag, "CNV overlaps with or is encapsulated by deletion"));
        header.addMetaDataLine(new VCFInfoHeaderLine("MATEID", VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.String, "ID of mate breakends"));
        header.addMetaDataLine(new VCFInfoHeaderLine("MATEDIST", 1, VCFHeaderLineType.Integer, "Distance to the mate breakend for mates on the same contig"));
        header.addMetaDataLine(new VCFFilterHeaderLine("Decoy", "Variant involves a decoy sequence"));
        header.addMetaDataLine(new VCFFilterHeaderLine("NearReferenceGap", "Variant is near (< 1000 bp) from a gap (run of >= 50 Ns) in the reference assembly"));
        header.addMetaDataLine(new VCFFilterHeaderLine("NearContigEnd", "Variant is near (< 1000 bp) from the end of a contig"));
        header.addMetaDataLine(new VCFFilterHeaderLine("InsufficientStrandEvidence", "Variant has insufficient number of reads per strand (< 1)."));
        header.addMetaDataLine(new VCFFilterHeaderLine("NotFullySpanned", "Duplication variant does not have any fully spanning reads."));
        header.addMetaDataLine(new VCFFormatHeaderLine("GT", 1, VCFHeaderLineType.String, "Genotype"));
        header.addMetaDataLine(new VCFFormatHeaderLine("AD", VCFHeaderLineCount.R, VCFHeaderLineType.Integer, "Read depth per allele"));
        header.addMetaDataLine(new VCFFormatHeaderLine("DP", 1, VCFHeaderLineType.Integer, "Read depth at this position for this sample"));
        header.addMetaDataLine(new VCFFormatHeaderLine("SAC", VCFHeaderLineCount.UNBOUNDED, VCFHeaderLineType.Integer, "Number of reads on the forward and reverse strand supporting each allele including reference"));
        header.addMetaDataLine(new VCFFormatHeaderLine("CN", 1, VCFHeaderLineType.Integer, "Copy number genotype for imprecise events"));

        return header;
    }
}
