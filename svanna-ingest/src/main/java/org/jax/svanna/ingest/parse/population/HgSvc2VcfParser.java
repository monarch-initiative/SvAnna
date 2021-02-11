package org.jax.svanna.ingest.parse.population;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypesContext;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.jax.svanna.core.reference.BasePopulationVariant;
import org.jax.svanna.core.reference.PopulationVariant;
import org.jax.svanna.core.reference.PopulationVariantOrigin;
import org.jax.svanna.ingest.parse.IOUtils;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.GenomicAssembly;
import org.monarchinitiative.svart.Variant;
import org.monarchinitiative.svart.util.VariantTrimmer;
import org.monarchinitiative.svart.util.VcfConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class HgSvc2VcfParser implements IngestRecordParser<PopulationVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HgSvc2VcfParser.class);

    private final GenomicAssembly genomicAssembly;

    private final Path hgsvcVcfPath;

    private final VcfConverter vcfConverter;

    public HgSvc2VcfParser(GenomicAssembly genomicAssembly, Path hgsvcVcfPath) {
        this.genomicAssembly = genomicAssembly;
        this.hgsvcVcfPath = hgsvcVcfPath;
        this.vcfConverter = new VcfConverter(genomicAssembly, VariantTrimmer.rightShiftingTrimmer(VariantTrimmer.removingCommonBase()));
    }


    @Override
    public Stream<? extends PopulationVariant> parse() throws IOException {
        VCFFileReader reader = new VCFFileReader(hgsvcVcfPath, false);
        CloseableIterator<VariantContext> variantIterator = reader.iterator();

        return variantIterator.stream()
                .onClose(IOUtils.close(reader))
                .map(toPopulationVariant())
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    private Function<VariantContext, Optional<? extends PopulationVariant>> toPopulationVariant() {
        return vc -> {
            Contig contig = genomicAssembly.contigByName(vc.getContig());
            if (contig.isUnknown()) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Unknown contig `{}` in record `{}`", vc.getContig(), vc);
                return Optional.empty();
            }

            if (vc.getAlternateAlleles().size() > 1) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Skipping multiallelic ({}) record `{}`", vc.getAlternateAlleles().size(), vc);
                return Optional.empty();
            }

            Allele alt = vc.getAlternateAllele(0);
            GenotypesContext genotypes = vc.getGenotypes();
            int altCount = 0;
            int calledAlleleCount = 0;
            for (Genotype genotype : genotypes) {
                altCount += genotype.countAllele(alt);
                for (Allele allele : genotype.getAlleles()) {
                    if (allele.isCalled()) {
                        calledAlleleCount += 1;
                    }
                }
            }

            if (calledAlleleCount == 0) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Skipping record with no ID `{}`", vc);
                return Optional.empty();
            }

            float alleleFrequency = 100.F * ((float) altCount) / calledAlleleCount;

            String id = vc.getAttributeAsString("ID", vc.getID());
            Variant variant = vcfConverter.convert(contig, id, vc.getStart(), vc.getReference().getDisplayString(), alt.getDisplayString());

            return Optional.of(BasePopulationVariant.of(variant.contig(), variant.strand(), variant.coordinateSystem(),
                    variant.startPosition(), variant.endPosition(),
                    variant.id(), variant.variantType(),
                    alleleFrequency, PopulationVariantOrigin.HGSVC2));
        };
    }
}
