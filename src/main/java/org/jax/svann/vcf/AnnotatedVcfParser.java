package org.jax.svann.vcf;


import org.jax.svann.except.SvAnnRuntimeException;
import org.jax.svann.lirical.LiricalHit;
import org.jax.svann.reference.Position;
import org.jax.svann.reference.genome.Contig;
import org.jax.svann.reference.genome.GenomeAssembly;
import org.jax.svann.reference.genome.GenomeAssemblyProvider;
import org.jax.svann.tspec.Enhancer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class intends to ingest a VCF file that has been annotated by Jannovar and to return
 * a list of variants in genes that match with the top candidates of a LIRICAL analysis. This
 * is clearly a clumsy way of doing things and is only intended to prototype/
 * TODO delete after refactoring enhancer code
 * @author Peter N Robinson
 */
@Deprecated
public class AnnotatedVcfParser {
    private final String annotatedVcfPath;
    /** Set of gene symbols of interest */
    private final Set<String> interestingGenes;
    private final Map<String, String> acc2chrMap;
    private final Map<String, List<LiricalHit>> hitmap = new HashMap<>();
    /** Non translocation lists */
    private final List<SvAnnOld> annlist = new ArrayList<>();
    private final List<SvAnnOld> translocationList = new ArrayList<>();
    private final Map<Contig, List<Enhancer>> chromosome2enhancerListMap;

    /**
     * For now, the enhancer files are provided only as hg38. TODO allow as parameter to CTOR
     */
    private final GenomeAssembly assembly = GenomeAssemblyProvider.getGrch38Assembly();

    public AnnotatedVcfParser(String annotatedVcf, List<LiricalHit> hitlist) {
        this.annotatedVcfPath = annotatedVcf;
        ChromosomeMapper cmap = new ChromosomeMapper();
        this.acc2chrMap = cmap.getAcc2chrMap();
        chromosome2enhancerListMap = new HashMap<>();
        interestingGenes = hitlist
                .stream()
                .map(LiricalHit::getGeneSymbols)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        for (LiricalHit h : hitlist) {
            Set<String> symset = h.getGeneSymbols();
            for (String s : symset) {
                hitmap.putIfAbsent(s, new ArrayList<>());
                hitmap.get(s).add(h);
            }
            Set<Enhancer> enh = h.getEnhancers();
            for (var e : enh) {
                chromosome2enhancerListMap.putIfAbsent(e.getChromosome(), new ArrayList<>());
                chromosome2enhancerListMap.get(e.getChromosome()).add(e);
            }
        }

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
                String chr = getChr(fields[0]);
                final Optional<Contig> contigOptional = assembly.getContigByName(chr);
                if (contigOptional.isEmpty()) {
                    continue;
                }
                final Contig contig = contigOptional.get();
                int pos = Integer.parseInt(fields[1]);
                String id = fields[2];
                String ref = fields[3];
                String alt = fields[4];
//                System.out.println(alt);
//                System.out.println(line);
//                for (int i=0;i<fields.length;i++) {
//                    System.out.printf("\t%d} %s\n", i, fields[i]);
//                }
                String qual = fields[5];
                String filter = fields[6];
                String info = fields[7];
                String format = fields[8];
                String gt = fields[9]; // assume just one sample for now
                SvAnnOld sva = new SvAnnOld(chr, pos, id, ref,alt,qual,filter,info,format,gt);

                List<Enhancer> enhancersOnSameChrom = chromosome2enhancerListMap.getOrDefault(contig, new ArrayList<>());
                for (var e : enhancersOnSameChrom) {
                    if (e.matchesPos(contig, Position.precise(pos))) {
                        sva.addEnhancerHit(e);
                    }
                }

                if (sva.isTranslocation()) {
                    translocationList.add(sva);
                } else {
                    Set<String> symbols = sva.getSymbols();
                    for (String sym : symbols) {
                        if (interestingGenes.contains(sym)) {
                            // a keeper
                            for (LiricalHit h : hitmap.get(sym)) {
                                sva.addLiricalHit(h);
                            }
                            annlist.add(sva);
                        }
                    }
                }
            }
        } catch (IOException e ) {
            e.printStackTrace();
        }
        Collections.sort(annlist, Collections.reverseOrder());
    }

    private String getChr(String acc) {
        if (acc.startsWith("chr")){
            return acc;
        }
        int i = acc.indexOf(".");
        if (i>0){
            acc= acc.substring(0,i);
        }
        if (this.acc2chrMap.containsKey(acc)) {
            return this.acc2chrMap.get(acc);
        } else {
            throw new SvAnnRuntimeException("Could not find acc" + acc);
        }
    }


    public List<SvAnnOld> getAnnlist() {
        return annlist;
    }

    public List<SvAnnOld> getTranslocationList() {
        return translocationList;
    }
}
