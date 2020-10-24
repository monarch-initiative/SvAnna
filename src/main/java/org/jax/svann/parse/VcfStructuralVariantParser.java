package org.jax.svann.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.structuralvar.SvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VcfStructuralVariantParser implements StructuralVariantParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(VcfStructuralVariantParser.class);

    private final GenomeAssembly assembly;

    public VcfStructuralVariantParser(GenomeAssembly assembly) {
        this.assembly = assembly;
    }

    @Override
    public ParseResult parseFile(Path filePath) {
        List<SvEvent> anns = new ArrayList<>();
        List<BreakendRecord> breakends = new ArrayList<>();

        try (VCFFileReader reader = new VCFFileReader(filePath, false)) {
            for (VariantContext vc : reader) {
                final SvType svType = SvType.fromString(vc.getAttributeAsString("SVTYPE", "UNKNOWN"));
                if (svType.equals(SvType.BND)) {
                    // process breakend record
                    final Optional<BreakendRecord> breakendOptional = parseBreakend(vc);
                    if (breakendOptional.isEmpty()) {
                        // parsing failed
                        continue;
                    }
                    breakendOptional.ifPresent(breakends::add);
                } else {
                    // process structural variants
                    parseStructuralVariant(vc, svType).ifPresent(anns::add);
                }
            }
        }

        return new ParseResult(anns, breakends);
    }

    private Optional<BreakendRecord> parseBreakend(VariantContext vc) {
        final Optional<Contig> contigOptional = assembly.getContigByName(vc.getContig());
        if (contigOptional.isEmpty()) {
            LOGGER.warn("Unknown contig `{}` for variant {}", vc.getContig(), vc);
            return Optional.empty();
        }
        final Contig contig = contigOptional.get();

        // position
        Position position;
        if (vc.hasAttribute("CIPOS")) {
            final List<Integer> cipos = vc.getAttributeAsIntList("CIPOS", 0);
            if (cipos.size() != 2) {
                LOGGER.warn("CIPOS size != 2 for variant {}", vc);
                return Optional.empty();
            } else {
                position = Position.imprecise(vc.getStart(), ConfidenceInterval.of(cipos.get(0), cipos.get(1)));
            }
        } else {
            position = Position.precise(vc.getStart(), CoordinateSystem.ONE_BASED);
        }

        // mate ID
        List<String> mateIds = vc.getAttributeAsStringList("MATEID", null);
        if (mateIds.size() > 1) {
            LOGGER.warn("Breakend with >1 mate IDs: {}", vc);
            return Optional.empty();
        }
        String mateId = mateIds.get(0);

        // ref & alt
        if (vc.getAlternateAlleles().size() != 1) {
            LOGGER.warn("Breakend with >1 alt allele: {}", vc);
            return Optional.empty();
        }
        final byte[] ref = vc.getReference().getDisplayBases();
        final byte[] alt = vc.getAlternateAllele(0).getDisplayBases();

        // in VCF specs, the position is always on the FWD(+) strand
        final ChromosomalPosition breakendPosition = ChromosomalPosition.of(contig, position, Strand.FWD);
        final BreakendRecord breakendRecord = new BreakendRecord(breakendPosition, vc.getID(), mateId, ref, alt);

        return Optional.of(breakendRecord);
    }

    private Optional<SvEvent> parseStructuralVariant(VariantContext vc, SvType svType) {
        // parse contig
        final Optional<Contig> contigOptional = assembly.getContigByName(vc.getContig());
        if (contigOptional.isEmpty()) {
            LOGGER.warn("Unknown contig `{}` for variant {}", vc.getContig(), vc);
            return Optional.empty();
        }
        final Contig contig = contigOptional.get();

        if (!vc.hasAttribute("END")) {
            LOGGER.warn("Missing `END` attribute for variant {}", vc);
            return Optional.empty();
        }

        // parse begin coordinate
        Position begin;
        if (vc.hasAttribute("CIPOS")) {
            final List<Integer> cipos = vc.getAttributeAsIntList("CIPOS", 0);
            if (cipos.size() == 2) {
                begin = Position.imprecise(vc.getStart(), ConfidenceInterval.of(cipos.get(0), cipos.get(1)), CoordinateSystem.ONE_BASED);
            } else {
                LOGGER.warn("CIPOS size != 2 for variant {}", vc);
                return Optional.empty();
            }
        } else {
            begin = Position.precise(vc.getStart(), CoordinateSystem.ONE_BASED);
        }

        // parse end coordinate
        Position end;
        int endPos = vc.getAttributeAsInt("END", 0);
        if (vc.hasAttribute("CIEND")) {
            final List<Integer> ciend = vc.getAttributeAsIntList("CIEND", 0);
            if (ciend.size() == 2) {
                end = Position.imprecise(endPos, ConfidenceInterval.of(ciend.get(0), ciend.get(1)), CoordinateSystem.ONE_BASED);
            } else {
                LOGGER.warn("CIEND size != 2 for variant {}", vc);
                return Optional.empty();
            }
        } else {
            end = Position.precise(endPos, CoordinateSystem.ONE_BASED);
        }

        // make the result
        final SvEvent svann = SvEvent.of(contig, begin, end, svType, Strand.FWD);
        return Optional.of(svann);
    }

}
