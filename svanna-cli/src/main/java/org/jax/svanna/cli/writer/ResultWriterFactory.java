package org.jax.svanna.cli.writer;

import org.jax.svanna.cli.writer.html.HtmlResultWriter;
import org.jax.svanna.cli.writer.tabular.TabularResultWriter;
import org.jax.svanna.core.hpo.PhenotypeDataService;
import org.jax.svanna.core.landscape.AnnotationDataService;
import org.jax.svanna.core.overlap.GeneOverlapper;

public class ResultWriterFactory {

    private final GeneOverlapper overlapper;
    private final AnnotationDataService annotationDataService;
    private final PhenotypeDataService phenotypeDataService;

    public ResultWriterFactory(GeneOverlapper overlapper, AnnotationDataService annotationDataService, PhenotypeDataService phenotypeDataService) {
        this.overlapper = overlapper;
        this.annotationDataService = annotationDataService;
        this.phenotypeDataService = phenotypeDataService;
    }


    public ResultWriter resultWriterForFormat(OutputFormat outputFormat) {
        switch (outputFormat) {
            case HTML:
                return new HtmlResultWriter(overlapper, annotationDataService, phenotypeDataService);
            case TSV:
                return new TabularResultWriter(OutputFormat.TSV.fileSuffix(), '\t', true);
            case CSV:
                return new TabularResultWriter(OutputFormat.CSV.fileSuffix(), ',', true);
            case VCF:
            default:
                throw new RuntimeException("Unsupported right now");
        }
    }

}
