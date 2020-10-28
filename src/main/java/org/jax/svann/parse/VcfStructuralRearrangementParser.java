package org.jax.svann.parse;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;
import org.jax.svann.reference.*;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class VcfStructuralRearrangementParser implements StructuralRearrangementParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(VcfStructuralRearrangementParser.class);

    private final GenomeAssembly assembly;

    private final BreakendAssembler assembler;

    public VcfStructuralRearrangementParser(GenomeAssembly assembly, BreakendAssembler assembler) {
        this.assembly = assembly;
        this.assembler = assembler;
    }

    @Override
    public Collection<SequenceRearrangement> parseFile(Path filePath) throws IOException {
        List<SequenceRearrangement> rearrangements = new ArrayList<>();
        List<BreakendRecord> breakendRecords = new ArrayList<>();
        try (VCFFileReader reader = new VCFFileReader(filePath, false)) {
            /*
            TODO: 28. 10. 2020 to parse events like:
             13 123456  INS0    T    C<ctg1> 6  PASS    SVTYPE=INS
             we the access to an assembly file with <ctg1> definition. Path to the file needs to be defined in VCF header.
             If the assembly is not defined, then we're most likely not able to proceed with these items.
             */

            for (VariantContext vc : reader) {
                SvType svType = SvType.fromString(vc.getAttributeAsString("SVTYPE", "UNKNOWN"));
                if (svType.equals(SvType.BND)) {
                    // decode the required information from this breakend record
                    parseBreakend(vc).ifPresent(breakendRecords::add);
                } else {
                    // process structural variants
                    parseStructuralVariant(vc, svType).ifPresent(rearrangements::add);
                }
            }
        }

        // now assemble all breakends into rearrangements
        List<SequenceRearrangement> assembled = assembler.assemble(breakendRecords);
        rearrangements.addAll(assembled);

        return rearrangements;
    }

    private Optional<BreakendRecord> parseBreakend(VariantContext vc) {
        Optional<Contig> contigOptional = assembly.getContigByName(vc.getContig());
        if (contigOptional.isEmpty()) {
            LOGGER.warn("Unknown contig `{}` for variant {}", vc.getContig(), vc);
            return Optional.empty();
        }
        Contig contig = contigOptional.get();
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

        // event ID
        String eventId = vc.getAttributeAsString("EVENT", null);

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
        final String ref = vc.getReference().getDisplayString();
        final String alt = vc.getAlternateAllele(0).getDisplayString();

        // in VCF specs, the position is always on the FWD(+) strand
        final ChromosomalPosition breakendPosition = ChromosomalPosition.of(contig, position, Strand.FWD);
        final BreakendRecord breakendRecord = new BreakendRecord(breakendPosition, vc.getID(), eventId, mateId, ref, alt);

        return Optional.of(breakendRecord);
    }

    /**
     * Process the variant into a structural rearrangement.
     *
     * @param vc     variant to be processed
     * @param svType previously parsed SV type to make the processing more convenient
     * @return optional with parsed rearrangement or empty optional if
     */
    private Optional<SequenceRearrangement> parseStructuralVariant(VariantContext vc, SvType svType) {
        List<Adjacency> adjacencies = new ArrayList<>();
        switch (svType) {
            // cases with a single adjacency
            case DELETION:
                makeDeletionAdjacency(vc).ifPresent(adjacencies::add);
                break;
            case DUPLICATION:
                makeDuplicationAdjacency(vc).ifPresent(adjacencies::add);
                break;
            // cases with two adjacencies
            case INSERTION:
                adjacencies.addAll(makeInsertionAdjacencies(vc));
                break;
            case INVERSION:
                adjacencies.addAll(makeInversionAdjacencies(vc));
                break;

            // these cases are not yet implemented
            case CNV:
            case DELETION_SIMPLE:
            case DELETION_TWISTED:
            case DEL_INV:
            case DUP_INS:
            case INV_DUP:
            case INV_INV_DUP:
                LOGGER.warn("Parsing of `{}` is not yet implemented: {}:{}-{}", svType, vc.getContig(), vc.getStart(), vc.getID());
                return Optional.empty();
            // cases we are not supposed to handle here
            case UNKNOWN:
            case TRANSLOCATION:
            case BND:
            default:
                LOGGER.warn("Unsupported SV type `{}` passed to `makeRearrangement`: {}:{}-{}", svType, vc.getContig(), vc.getStart(), vc.getID());
                return Optional.empty();
        }

        return Optional.of(SimpleSequenceRearrangement.of(svType, adjacencies));
    }

    Optional<Adjacency> makeDeletionAdjacency(VariantContext vc) {
        // We know that this context represents symbolic deletion.
        // Let's get the required coordinates first
        return extractCoreData(vc).map(coords -> {
            // then convert the coordinates to adjacency
            Contig contig = coords.getContig();

            ChromosomalPosition leftPos = ChromosomalPosition.of(contig,
                    Position.imprecise(coords.getBegin().getPos() - 1, coords.getBegin().getConfidenceInterval()),
                    Strand.FWD);
            SimpleBreakend left = SimpleBreakend.of(leftPos, vc.getID(), vc.getReference().getDisplayString());

            ChromosomalPosition rightPos = ChromosomalPosition.of(contig,
                    Position.imprecise(coords.getEnd().getPos() + 1, coords.getEnd().getConfidenceInterval()),
                    Strand.FWD);
            SimpleBreakend right = SimpleBreakend.of(rightPos, vc.getID());

            return SimpleAdjacency.empty(left, right);
        });
    }

    Optional<Adjacency> makeDuplicationAdjacency(VariantContext vc) {
        /*
        We know that this context represents a symbolic inversion - SvType.DUPLICATION
        Duplication consists of 2 adjacencies that we denote as alpha, and beta.
        The VCF line might look like this:
        2	11	DUP0	T	<DUP>	6	PASS	SVTYPE=DUP;END=19;CIPOS=-2,2;CIEND=-1,1
         */
        return extractCoreData(vc).map(cd -> {
            String id = vc.getID();
            Contig contig = cd.getContig();
            Position begin = cd.getBegin();
            Position end = cd.getEnd();

            // the 1st adjacency (alpha) starts at the end coordinate, by convention on + strand
            ChromosomalPosition alphaLeftPos = ChromosomalPosition.of(contig, end, Strand.FWD);
            SimpleBreakend alphaLeft = SimpleBreakend.of(alphaLeftPos, id);
            // the right position is the begin position of the duplicated segment on + strand
            ChromosomalPosition alphaRightPos = ChromosomalPosition.of(contig, begin, Strand.FWD);
            SimpleBreakend alphaRight = SimpleBreakend.of(alphaRightPos, id);
            return SimpleAdjacency.empty(alphaLeft, alphaRight);
        });
    }

    List<? extends Adjacency> makeInversionAdjacencies(VariantContext vc) {
        /*
        We know that this context represents a symbolic inversion - SvType.INVERSION
        Inversion consists of 2 adjacencies, we denote them as alpha and beta
        The VCF line might look like this:
        2   11  INV0    T   <INV>   6   PASS    SVTYPE=INV;END=19
        */
        Optional<CoreData> cdOpt = extractCoreData(vc);
        if (cdOpt.isEmpty()) {
            return List.of();
        }

        String id = vc.getID();
        CoreData cd = cdOpt.get();
        Contig contig = cd.getContig();
        Position begin = cd.getBegin();
        Position end = cd.getEnd();

        // TODO: 27. 10. 2020 this method involves shifting coordinates +- 1 base pair, while not adjusting the CIs.
        //  This should not be an issue, but evaluate just to be sure

        // the 1st adjacency (alpha) starts one base before begin coordinate (POS), by convention on the (+) strand
        ChromosomalPosition alphaLeftPos = ChromosomalPosition.of(contig,
                Position.imprecise(begin.getPos() - 1, begin.getConfidenceInterval()),
                Strand.FWD);
        Breakend alphaLeft = SimpleBreakend.of(alphaLeftPos, id, vc.getReference().getDisplayString());
        // the right position is the last base of the inverted segment on the (-) strand
        ChromosomalPosition alphaRightPos = ChromosomalPosition.of(contig, end, Strand.FWD).withStrand(Strand.REV);
        SimpleBreakend alphaRight = SimpleBreakend.of(alphaRightPos, id);
        Adjacency alpha = SimpleAdjacency.empty(alphaLeft, alphaRight);


        // the 2nd adjacency (beta) starts at the begin coordinate on (-) strand
        ChromosomalPosition betaLeftPos = ChromosomalPosition.of(contig, begin, Strand.FWD).withStrand(Strand.REV);
        SimpleBreakend betaLeft = SimpleBreakend.of(betaLeftPos, id);
        // the right position is one base past end coordinate, by convention on (+) strand
        ChromosomalPosition betaRightPos = ChromosomalPosition.of(contig,
                Position.imprecise(end.getPos() + 1, end.getConfidenceInterval()),
                Strand.FWD);
        Breakend betaRight = SimpleBreakend.of(betaRightPos, id);
        Adjacency beta = SimpleAdjacency.empty(betaLeft, betaRight);
        return List.of(alpha, beta);
    }

    List<? extends Adjacency> makeInsertionAdjacencies(VariantContext vc) {
        /*
        We know that this context represents a symbolic insertion - SvType.INSERTION
        Insertion consists of 2 adjacencies, we denote them as alpha and beta
        The VCF line might look like this:
        2   15  INS0    T   <INS>   6   PASS    SVTYPE=INS;END=15;SVLEN=10

        Note that `SVLEN` field is mandatory, otherwise it is not possible to determine length of the insertion.
        */
        if (!vc.hasAttribute("SVLEN")) {
            LOGGER.warn("Missing `SVLEN` attribute for an insertion: {}:{}-{}", vc.getContig(), vc.getStart(), vc.getID());
            return List.of();
        }
        int insLength = vc.getAttributeAsInt("SVLEN", -1);

        Optional<CoreData> cdOpt = extractCoreData(vc);
        if (cdOpt.isEmpty()) {
            return List.of();
        }


        String id = vc.getID();
        CoreData cd = cdOpt.get();
        Contig contig = cd.getContig();
        Position begin = cd.getBegin();

        Contig insContig = new InsertionContig(id, insLength);

        // the 1st adjacency (alpha) starts at the POS coordinate, by convention on the (+) strand
        ChromosomalPosition alphaLeftPos = ChromosomalPosition.of(contig, begin, Strand.FWD);
        Breakend alphaLeft = SimpleBreakend.of(alphaLeftPos, id, vc.getReference().getDisplayString());
        // the right position is the first base of the insertion contig
        ChromosomalPosition alphaRightPos = ChromosomalPosition.of(insContig, Position.precise(1), Strand.FWD);
        SimpleBreakend alphaRight = SimpleBreakend.of(alphaRightPos, id);
        Adjacency alpha = SimpleAdjacency.empty(alphaLeft, alphaRight);

        // the 2nd adjacency (beta) starts at the end of the insertion contig
        ChromosomalPosition betaLeftPos = ChromosomalPosition.of(insContig, Position.precise(insLength), Strand.FWD);
        SimpleBreakend betaLeft = SimpleBreakend.of(betaLeftPos, id);
        // the right position is one base past the POS coordinate, by convention on (+) strand
        ChromosomalPosition betaRightPos = ChromosomalPosition.of(contig, Position.imprecise(begin.getPos() + 1, begin.getConfidenceInterval()), Strand.FWD);
        Breakend betaRight = SimpleBreakend.of(betaRightPos, id);
        Adjacency beta = SimpleAdjacency.empty(betaLeft, betaRight);
        return List.of(alpha, beta);
    }

    /**
     * Convenience method to extract contig, start and `END` (+confidence intervals) values from variant context.
     */
    private Optional<CoreData> extractCoreData(VariantContext vc) {
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

        return Optional.of(new CoreData(contig, begin, end));
    }

}
