package org.monarchinitiative.svanna.cli.writer.html.svg;

import org.monarchinitiative.svanna.model.landscape.dosage.Dosage;
import org.monarchinitiative.svanna.model.landscape.dosage.DosageRegion;
import org.monarchinitiative.svanna.model.landscape.dosage.DosageSensitivity;
import org.monarchinitiative.svanna.model.landscape.dosage.DosageSensitivityEvidence;
import org.monarchinitiative.svart.CoordinateSystem;
import org.monarchinitiative.svart.Coordinates;
import org.monarchinitiative.svart.GenomicRegion;
import org.monarchinitiative.svart.Strand;
import org.monarchinitiative.svart.assembly.GenomicAssemblies;
import org.monarchinitiative.svart.assembly.GenomicAssembly;

public class TestData {

    private static final GenomicAssembly GRCH38 = GenomicAssemblies.GRCh38p13();

    private TestData() {
    }

    public static DosageRegion gckHaploinsufficiency() {
        return DosageRegion.of(
                GenomicRegion.of(GRCH38.contigByName("9"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 44_142_000, 44_200_170)),
                Dosage.of("gck-haploinsufficiency", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE));
    }

    public static DosageRegion surf2Haploinsufficiency() {
        return DosageRegion.of(
                GenomicRegion.of(GRCH38.contigByName("9"), Strand.POSITIVE, Coordinates.of(CoordinateSystem.zeroBased(), 133_356_500, 133_361_500)),
                Dosage.of("surf2-haploinsufficiency", DosageSensitivity.HAPLOINSUFFICIENCY, DosageSensitivityEvidence.SUFFICIENT_EVIDENCE));
    }
}
