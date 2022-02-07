package org.jax.svanna.ingest.parse.population;

import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.jax.svanna.core.LogUtils;
import org.jax.svanna.ingest.parse.IOUtils;
import org.jax.svanna.model.landscape.variant.BasePopulationVariant;
import org.jax.svanna.model.landscape.variant.PopulationVariant;
import org.jax.svanna.model.landscape.variant.PopulationVariantOrigin;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.GenomicVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class DbsnpVcfParser extends AbstractVcfIngestRecordParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbsnpVcfParser.class);

    // TODO - what is the real value here? What frequency should we use when CAF==. for an allele?
    private static final float DEFAULT_FREQ = 1e-2F;

    private final Path dbSnpPath;

    public DbsnpVcfParser(GenomicAssembly assembly, Path dbSnpPath) {
        super(assembly);
        this.dbSnpPath = dbSnpPath;
    }

    @Override
    public Stream<? extends PopulationVariant> parse() throws IOException {
        VCFFileReader reader = new VCFFileReader(dbSnpPath, false);
        CloseableIterator<VariantContext> variantIterator = reader.iterator();

        return variantIterator.stream()
                .onClose(IOUtils.close(reader))
                .map(toPopulationVariant())
                .flatMap(Collection::stream);

    }

    protected Function<VariantContext, Collection<? extends PopulationVariant>> toPopulationVariant() {
        return vc -> {
            Contig contig = vcfConverter.parseContig(vc.getContig());
            if (contig.isUnknown()) {
                LogUtils.logWarn(LOGGER, "Unknown contig `{}` in record `{}`", vc.getContig(), vc);
                return List.of();
            }

            if (!vc.getCommonInfo().hasAttribute("CAF")) {
                LogUtils.logWarn(LOGGER, "Skipping variant `{}` with no CAF field", vc.getID());
                return List.of();
            }
            List<String> caf = vc.getCommonInfo().getAttributeAsStringList("CAF", ".");
            if (caf.size() != vc.getNAlleles()) {
                LogUtils.logWarn(LOGGER, "Expected {} frequencies, got {} ({}) for variant ", vc.getNAlleles(), caf.size(), caf, vc.getID());
                return List.of();
            }

            List<Allele> alleles = vc.getAlleles();
            if (alleles.size() < 2) {
                LogUtils.logWarn(LOGGER, "Cannot process variant {} with {} (<2) allele(s)", vc.getID(), vc.getNAlleles());
                return List.of();
            }

            List<PopulationVariant> variants = new ArrayList<>(alleles.size() - 1);
            Allele ref = vc.getReference();
            for (int i = 1, nAlleles = alleles.size(); i < nAlleles; i++) {
                Allele alt = alleles.get(i);
                if (ref.length() == 1 && ref.length() == alt.length() )
                    continue; // ignore SNVs

                GenomicVariant variant = vcfConverter.convert(contig, vc.getID(), vc.getStart(), ref.getDisplayString(), alt.getDisplayString());

                String frequency = caf.get(i);
                float alleleFrequency = frequency.equals(".") ? DEFAULT_FREQ : 100 * Float.parseFloat(frequency);

                BasePopulationVariant populationVariant = BasePopulationVariant.of(
                        GenomicRegion.of(variant.contig(), variant.strand(), variant.coordinateSystem(), variant.start(), variant.end()),
                        variant.id(), variant.variantType(), alleleFrequency, PopulationVariantOrigin.DBSNP);

                variants.add(populationVariant);
            }

            return variants;
        };
    }
}
