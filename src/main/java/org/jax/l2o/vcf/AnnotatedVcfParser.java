package org.jax.l2o.vcf;


import org.jax.l2o.lirical.LiricalHit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class intends to ingest a VCF file that has been annotated by Jannovar and to return
 * a list of variants in genes that match with the top candidates of a LIRICAL analysis. This
 * is clearly a clumsy way of doing things and is only intended to prototype/
 * @author Peter N Robinson
 */
public class AnnotatedVcfParser {
    private final String annotatedVcfPath;
    /** Set of gene symbols of interest */
    private final Set<String> interestingGenes;

    public AnnotatedVcfParser(String annotatedVcf, List<LiricalHit> hitlist) {
        this.annotatedVcfPath = annotatedVcf;
        interestingGenes = hitlist
                .stream()
                .map(LiricalHit::getGeneSymbols)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        try (BufferedReader br = new BufferedReader(new FileReader(annotatedVcfPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String [] fields = line.split("\t");
                if (fields.length < 10) {
                    System.err.printf("[ERROR] Bad VCF line with only %d fields: %s", fields.length, line);
                    continue;
                }
                String chr = fields[0];
                int pos = Integer.parseInt(fields[1]);
                String id = fields[2];
                String ref = fields[3];
                String alt = fields[4];
                String qual = fields[5];
                String filter = fields[6];
                String info = fields[7];
                String format = fields[8];
                String gt = fields[9]; // assume just one sample for now
                SvAnn sva = new SvAnn(chr, pos, id, ref,alt,qual,filter,info,format,gt);
                Set<String> symbols = sva.getSymbols();
                for (String sym : symbols) {
                    if (interestingGenes.contains(sym)) {
                        // a keeper
                        System.out.println("YES"+sva);
                    }
                }
                //System.out.println(line);
            }
        } catch (IOException e ) {
            e.printStackTrace();
        }
    }
}
