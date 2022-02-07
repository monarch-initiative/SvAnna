package org.jax.svanna.ingest.parse.population;

import org.jax.svanna.ingest.parse.IngestRecordParser;
import org.jax.svanna.model.landscape.variant.PopulationVariant;
import org.monarchinitiative.svart.assembly.GenomicAssembly;
import org.monarchinitiative.svart.util.VariantTrimmer;
import org.monarchinitiative.svart.util.VcfConverter;

public abstract class AbstractVcfIngestRecordParser implements IngestRecordParser<PopulationVariant> {

    protected final VcfConverter vcfConverter;

    protected AbstractVcfIngestRecordParser(GenomicAssembly genomicAssembly) {
        this.vcfConverter = new VcfConverter(genomicAssembly, VariantTrimmer.rightShiftingTrimmer(VariantTrimmer.removingCommonBase()));
    }
}
