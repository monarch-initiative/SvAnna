package org.jax.svanna.benchmark.cmd.remap.write;

import org.jax.svanna.io.FullSvannaVariant;
import org.monarchinitiative.svart.Contig;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.GenomicVariant;
import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.assembly.SequenceRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public class BedWriter implements FullSvannaVariantWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BedWriter.class);

    private final Path output;
    private final SvTool tool;

    public BedWriter(Path output, SvTool tool) {
        this.output = Objects.requireNonNull(output, "Output must not be null");
        this.tool = Objects.requireNonNull(tool, "Tool must not be null");
    }


    @Override
    public int write(Iterable<FullSvannaVariant> variants) {
        int written = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            for (FullSvannaVariant variant : variants) {
                Optional<String> lineOptional;
                switch (tool) {
                    case X_CNV:
                        lineOptional = formatToXCnvBed(variant);
                        break;
                    case CLASSIFY_CNV:
                        lineOptional = formatToClassifyCnv(variant);
                        break;
                    default:
                        continue;
                }

                if (lineOptional.isPresent()) {
                    String line = lineOptional.get();
                    try {
                        writer.write(line);
                        writer.newLine();
                        written++;
                    } catch (IOException e) {
                        LOGGER.warn("Error writing out line {}", e.getMessage(), e);
                    }
                }
            }
            return written;
        } catch (IOException e) {
            LOGGER.error("Error: {}", e.getMessage(), e);
            return 0;
        }
    }

    private static Optional<String> formatToClassifyCnv(FullSvannaVariant variant) {
        /*
        From here - https://github.com/Genotek/ClassifyCNV/blob/master/Examples/ACMG_examples.hg19.bed

        chr12	27715516	29628080	DEL
        chr17	41784108	42438203	DUP
        chr19	43242796	43741310	DEL
        chr22	18912231	21465672	DUP
        chr12	12864101	14983330	DEL
        chr11	45904399	46480747	DEL
        chr18	53049652	53134356	DEL
        chr22	40654201	40659533	DEL
        chr15	30507853	30807921	DEL
        chr1	1379519	1435227	DEL
        chr13	20812000	21012000	DEL
        chr17	553250	1353250	DEL
        chr22	31645547	32807482	DEL
        chr9	108597937	111269478	DEL
        chrX	23223505	23660309	DEL
        chr5	125989631	126295396	DUP
        chr2	45408934	45976420	DUP
        chr4	69373811	69491113	DEL
        chr19	18291753	18311626	DUP
        chr3	190380498	191783134	DEL
        chr17	13029888	14707559	DUP
         */
        GenomicVariant gv = variant.genomicVariant();
        String contigName = gv.contig().ucscName();
        int start = gv.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased());
        int end = gv.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.oneBased());
        switch (gv.variantType().baseType()) {
            case DUP:
                return Optional.of(String.format("%s\t%d\t%d\t%s", contigName, start, end, "DUP"));
            case DEL:
                return Optional.of(String.format("%s\t%d\t%d\t%s", contigName, start, end, "DEL"));
            default:
                return Optional.empty();
        }
    }

    private static Optional<String> formatToXCnvBed(FullSvannaVariant variant) {
        /*
        From here - https://github.com/kbvstmd/XCNV/blob/master/example_data/2.bed

        1	11111122	11122222	gain
        2	2212312	2332212	loss
        2	2222999	3000222	gain
         */
        GenomicVariant gv = variant.genomicVariant();
        Contig contig = gv.contig();
        if (!contig.sequenceRole().equals(SequenceRole.ASSEMBLED_MOLECULE))
            // X-CNV does not work with non-canonical contigs
            return Optional.empty();
        String contigName = contig.name();
        int start = gv.startOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        int end = gv.endOnStrandWithCoordinateSystem(Strand.POSITIVE, CoordinateSystem.zeroBased());
        switch (gv.variantType().baseType()) {
            case DUP:
                return Optional.of(String.format("%s\t%d\t%d\t%s", contigName, start, end, "gain"));
            case DEL:
                return Optional.of(String.format("%s\t%d\t%d\t%s", contigName, start, end, "loss"));
            default:
                return Optional.empty();
        }
    }
}
