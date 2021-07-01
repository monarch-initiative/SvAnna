package org.jax.svanna.ingest.parse.population;

import org.jax.svanna.core.landscape.BasePopulationVariant;
import org.jax.svanna.core.landscape.PopulationVariant;
import org.jax.svanna.core.landscape.PopulationVariantOrigin;
import org.jax.svanna.ingest.parse.IOUtils;
import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.monarchinitiative.svart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class DgvFileParser implements IngestRecordParser<PopulationVariant> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DgvFileParser.class);

    private final GenomicAssembly genomicAssembly;

    private final Path dgvFile;

    public DgvFileParser(GenomicAssembly genomicAssembly, Path dgvFile) {
        this.genomicAssembly = genomicAssembly;
        this.dgvFile = dgvFile;
    }


    @Override
    public Stream<? extends PopulationVariant> parse() throws IOException {
        BufferedReader reader = Files.newBufferedReader(dgvFile);
        return reader.lines()
                .onClose(IOUtils.close(reader))
                .skip(1) // header
                .map(toVariants())
                .flatMap(Collection::stream);
    }


    private Function<String, Collection<? extends PopulationVariant>> toVariants() {
        return line -> {
            // lines in dgv txt file look like:
            // variantaccession        chr     start   end     varianttype     variantsubtype  reference       pubmedid        method  platform        mergedvariants  supportingvariants      mergedorsample  frequency    samplesize      observedgains   observedlosses  cohortdescription       genes   samples
            // nsv482937       1       1       2300000 CNV     loss    Iafrate_et_al_2004      15286789        BAC aCGH,FISH                   nssv2995976     M               39      0       1               ACAP3,AGRN,ANKRD65,ATAD3A,ATAD3B,ATAD3C,AURKAIP1,B3GALT6,C1orf159,C1orf170,C1orf233,C1orf86,CALML6,CCNL2,CDK11A,CDK11B,CPSF3L,DDX11L1,DVL1,FAM132A,FAM138A,FAM138F,FAM41C,FAM87B,GABRD,GLTPD1,GNB1,HES4,ISG15,KIAA1751,KLHL17,LINC00115,LINC01128,LOC100129534,LOC100130417,LOC100132062,LOC100132287,LOC100133331,LOC100288069,WASH7P
            String[] arr = line.split("\t", -1); // the last column of some rows is empty. Still we want it
            // we expect each line to have at least 20 columns
            if (arr.length < 20) {
                LOGGER.warn("Expected at least 20 columns, got '{}' in line '{}'", arr.length, line);
                return List.of();
            }

            // parse coordinates first
            if(arr[1].isBlank()){
                if (LOGGER.isDebugEnabled()) LOGGER.debug("Skipping line with missing contig `{}`", line);
                return List.of();
            }

            String contigName = "chr" + arr[1];
            Contig contig = genomicAssembly.contigByName(contigName);
            if (contig.isUnknown()) {
                LOGGER.debug("Skipping unknown contig '{}' in line '{}'", contigName, line);
                return List.of();
            }

            Position start, end;
            CoordinateSystem coordinateSystem = CoordinateSystem.oneBased();
            try {
                // coordinates are 1-based, inclusive
                start = Position.of(Integer.parseInt(arr[2]));
                end = Position.of(Integer.parseInt(arr[3]));
            } catch (NumberFormatException e) {
                LOGGER.warn("Unable to parse coordinates of line '{}'", line);
                return List.of();
            }

            // then parse sv type and subtype
            VariantType type = parseSvType(arr[4], arr[5]);

            // now add DGV specific attributes/values to DgvFeature

            // variantaccession
            String variantAccession = arr[0];

            // pubmed id
//        String pubmedId = arr[7];

            // method
//        Set<ExpMethodType> discoveryMethods = ExpMethodType.decodeFromString(arr[8], ",");
            float sampleSize = Float.NaN, observedGains = Float.NaN, observedLosses = Float.NaN;
            try {
                /*
                 * NOTE: we assume that sample size equals to allele count.
                 */
                sampleSize = Float.parseFloat(arr[14]);
            } catch (NumberFormatException nfe) {
                LOGGER.warn("problem parsing sample size integer in {}", line);
            }

            try {
                if (!arr[15].isEmpty()) { // the field is empty sometimes
                    observedGains = Float.parseFloat(arr[15]);
                }
            } catch (NumberFormatException nfe) {
                LOGGER.warn("problem parsing observed gains integer in {}", line);
            }
            try {
                if (!arr[16].isEmpty()) { // the field is empty sometimes
                    observedLosses = Float.parseFloat(arr[16]);
                }
            } catch (NumberFormatException nfe) {
                LOGGER.warn("problem parsing observed losses integer in {}", line);
            }

            float gainsFreq = 100 * observedGains / sampleSize;
            float lossesFreq = 100 * observedLosses / sampleSize;
            if (arr[5].equals("gain+loss")) {
                return List.of(
                        BasePopulationVariant.of(contig, Strand.POSITIVE, coordinateSystem, start, end, variantAccession, VariantType.DUP, gainsFreq, PopulationVariantOrigin.DGV),
                        BasePopulationVariant.of(contig, Strand.POSITIVE, coordinateSystem, start, end, variantAccession, VariantType.DEL, lossesFreq, PopulationVariantOrigin.DGV));
            } else {
                float frequency = Float.NaN;
                switch (type.baseType()) {
                    case DEL:
                        frequency = lossesFreq; // !!frequencies are percentages!!
                        break;
                    case DUP:
                    case INS:
                        frequency = gainsFreq; // !!frequencies are percentages!!
                        break;
                    default:
                        // fall through
                        break;
                }
                return List.of(BasePopulationVariant.of(contig, Strand.POSITIVE, coordinateSystem, start, end, variantAccession, type, frequency, PopulationVariantOrigin.DGV));
            }
        };
    }

    static VariantType parseSvType(String svType, String svSubtype) {
        switch (svType.toLowerCase()) {
            case "cnv":
                // Quoth VCF 4.2 spec:
                // The CNV category should not be used when a more specific category can be applied.
                // Let's try to find a more specific category:
                switch (svSubtype.toLowerCase()) {
                    // http://dgv.tcag.ca/dgv/app/faq
                    // Variant Type
                    // CNV: A genetic variation involving a net gain or loss of DNA compared to a
                    // reference sample or assembly. OTHER: A general category that represents variants within a complex region and also includes inversions.
                    // Variant Subtype
                    // CNV = a copy number variation, with unknown properties
                    // Complex= combination of multiple variant_sub_types
                    // Deletion = a net loss of DNA
                    // Duplication = a gain of an extra copy of DNA
                    // Gain = a net gain of DNA
                    // Gain+Loss = variant region where some samples have a net gain in DNA while other samples have a net loss
                    // Insertion = insertion of additional DNA sequence relative to a reference assembly
                    // Loss = net loss of DNA
                    // OTHER Complex = complex region involving multiple variant types (and or variant sub types).
                    // OTHER Inversion = a region where the orientation has been flipped compared to the reference assembly
                    // OTHER Tandem duplication = a duplication of a region which has been inserted next to the original in a tandem arrangement.
                    // The terms deletion and loss are equivalent. Array based studies tended to use the term loss, while sequencing based approaches tend
                    // to report variants using the term deletion. Moving forward we will work to standardize the terms to reduce any ambiguity.
                    // The terms gain and duplication are also equivalent. The terminology used by the submitting authors follow the same pattern and logic
                    // as for deletions/loss.
                    // The term Gain+loss is used for variant regions (merged/summarized across all samples in a study), where there are a number of supporting
                    // (sample level) calls where some individuals have a gain/duplication, while others have a loss/deletion. These are multiallelic sites and
                    // the variant region contains samples with both Gains and Losses.
                    // Complex variants are variant regions where there may be a combination of different variant types at the same locus (combination of inversion
                    // and deletion for example), either within the same sample and/or across multiple samples.
                    case "deletion":
                    case "loss": // "The terms deletion and loss are equivalent."
                        return VariantType.DEL;
                    case "mobile element deletion":
                        return VariantType.DEL_ME;
                    case "herv deletion":
                        return VariantType.DEL_ME_HERV;
                    case "alu deletion":
                        return VariantType.DEL_ME_ALU;
                    case "line1 deletion":
                        return VariantType.DEL_ME_LINE1;
                    case "sva deletion":
                        return VariantType.DEL_ME_SVA;
                    case "duplication":
                    case "gain": // "The terms gain and duplication are also equivalent."
                        return VariantType.DUP;
                    case "gain+loss":
                        return VariantType.CNV;
                    case "insertion":
                    case "novel sequence insertion":
                        return VariantType.INS;
                    case "line1 insertion":
                        return VariantType.INS_ME_LINE1;
                    case "alu insertion":
                        return VariantType.INS_ME_ALU;
                    case "sva insertion":
                        return VariantType.INS_ME_SVA;
                    case "herv insertion":
                        return VariantType.INS_ME_HERV;
                    case "mobile element insertion":
                        return VariantType.INS_ME;
                    case "tandem duplication":
                        return VariantType.DUP_TANDEM;
                    default:
                        LOGGER.warn("Unknown CNV subtype '{}'", svSubtype);
                        return VariantType.CNV;
                }
            case "other":
                switch (svSubtype.toLowerCase()) {
                    case "complex":
                    case "sequence alteration":
                        return VariantType.UNKNOWN;
                    case "inversion":
                        return VariantType.INV;
                    default:
                        LOGGER.warn("Unknown OTHER subtype '{}'", svSubtype);
                        return VariantType.UNKNOWN;
                }
            default:
                LOGGER.warn("Unknown SV Type '{}'", svType);
                return VariantType.UNKNOWN;
        }
    }
}
