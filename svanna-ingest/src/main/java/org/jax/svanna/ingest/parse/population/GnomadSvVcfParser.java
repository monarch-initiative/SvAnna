package org.jax.svanna.ingest.parse.population;

import htsjdk.samtools.liftover.LiftOver;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.samtools.util.Interval;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.jax.svanna.core.reference.BasePopulationVariant;
import org.jax.svanna.core.reference.PopulationVariant;
import org.jax.svanna.core.reference.PopulationVariantOrigin;
import org.jax.svanna.ingest.parse.IOUtils;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class GnomadSvVcfParser implements IngestRecordParser<PopulationVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GnomadSvVcfParser.class);

    private static final double DEFAULT_AF = 1E-5;

    private final GenomicAssembly genomicAssembly;

    private final Path gnomadSvVcfPath;

    private final LiftOver liftOver;

    private final AtomicInteger liftoverFailedCounter = new AtomicInteger();

    public GnomadSvVcfParser(GenomicAssembly genomicAssembly, Path gnomadSvVcfPath, Path hg19ToHg38Chain) {
        this.genomicAssembly = genomicAssembly;
        this.gnomadSvVcfPath = gnomadSvVcfPath;
        this.liftOver = new LiftOver(hg19ToHg38Chain.toFile());
    }

    @Override
    public Stream<PopulationVariant> parse() throws IOException {
        VCFFileReader reader = new VCFFileReader(gnomadSvVcfPath, false);
        CloseableIterator<VariantContext> variantIterator = reader.iterator();
        return variantIterator.stream()
                .onClose(IOUtils.close(reader, variantIterator))
                    .filter(multiallelicPcrPlusPredictedGenotypingArtifacts())
                    .filter(unresolvedBreakends())
                    .map(toPopulationVariant())
                    .filter(Optional::isPresent)
                    .map(Optional::get);
    }

    /**
     *
     * @return Function for mapping {@link VariantContext} to {@link Optional} of {@link PopulationVariant}.
     * The <code>vc</code> must not represent multi-allelic SV
     */
    private Function<VariantContext, Optional<PopulationVariant>> toPopulationVariant() {
        return vc -> {
            if (vc.getAlternateAlleles().size() > 1) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Skipping multiallelic record `{}`", vc.getID());
                return Optional.empty();
            }

            if (!vc.hasAttribute("END")) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Skipping record `{}` with missing `END`", vc.getID());
                return Optional.empty();
            }

            int s = vc.getStart();
            int e = vc.getAttributeAsInt("END", -1);
            String ucscName = "chr" + vc.getContig(); // liftover is UCSC's tool, hence it always uses `chr` prefix

            Interval lifted = liftOver.liftOver(new Interval(ucscName, s, e, false, vc.getID()));
            if (lifted == null) {
                if (LOGGER.isDebugEnabled())
                    LOGGER.debug("Liftover of `{}` failed", vc.getID());
                liftoverFailedCounter.incrementAndGet();
                return Optional.empty();
            }
            Contig contig = genomicAssembly.contigByName(lifted.getContig());
            if (contig == Contig.unknown()) {
                if (LOGGER.isWarnEnabled())
                    LOGGER.warn("Skipping record {} due to unknown contig '{}' ", vc.getID(), lifted.getContig());
                return Optional.empty();
            }

            Position start = Position.of(lifted.getStart());
            Position end = Position.of(lifted.getEnd());
            VariantType variantType = VariantType.parseType(vc.getReference().getDisplayString(), vc.getAlternateAllele(0).getDisplayString());

            float popmaxAf = 100.F * (float) vc.getAttributeAsDouble("POPMAX_AF", DEFAULT_AF); // !!frequencies are percentages!!

            return Optional.of(BasePopulationVariant.of(contig, Strand.POSITIVE, CoordinateSystem.oneBased(), start, end,
                    vc.getID(), variantType, popmaxAf, PopulationVariantOrigin.GNOMAD_SV));
        };
    }

    /**
     * @return Predicate for removing variants containing one of
     * <ul>
     * <li><code>MULTIALLELIC</code>,</li>
     * <li><code>PCRPLUS_ENRICHED</code>, or</li>
     * <li><code>PREDICTED_GENOTYPING_ARTIFACT</code></li>
     * </ul>
     * fields.
     */
    private static Predicate<? super VariantContext> multiallelicPcrPlusPredictedGenotypingArtifacts() {
        return vc -> !(vc.getFilters().contains("MULTIALLELIC")
                || vc.getFilters().contains("PCRPLUS_ENRICHED")
                || vc.getFilters().contains("PREDICTED_GENOTYPING_ARTIFACT"));
    }

    /**
     * @return Predicate for removing variants marked with <code>UNRESOLVED</code> filter, and having <code>SVTYPE=BND</code>
     */
    static Predicate<? super VariantContext> unresolvedBreakends() {
        return vc -> !(vc.getFilters().contains("UNRESOLVED")
                && vc.getAttributeAsString("SVTYPE", "NA").equals("BND"));
    }
}
