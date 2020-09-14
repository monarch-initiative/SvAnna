package org.jax.l2o.vcf;

import java.util.HashMap;
import java.util.Map;

public class ChromosomeMapper {
    /*
    CM000663 	NC_000001 	Chromosome 1
CM000664 	NC_000002 	Chromosome 2
CM000665 	NC_000003 	Chromosome 3
CM000666 	NC_000004 	Chromosome 4
CM000667 	NC_000005 	Chromosome 5
CM000668 	NC_000006 	Chromosome 6
CM000669 	NC_000007 	Chromosome 7
CM000670 	NC_000008 	Chromosome 8
CM000671 	NC_000009 	Chromosome 9


GL000250 	NT_167244 	MHC Region (ALT_REF_LOCI_1)
GL000251 	NT_113891 	MHC Region (ALT_REF_LOCI_2)
GL000252 	NT_167245 	MHC Region (ALT_REF_LOCI_3)
GL000253 	NT_167246 	MHC Region (ALT_REF_LOCI_4)
GL000254 	NT_167247 	MHC Region (ALT_REF_LOCI_5)
GL000255 	NT_167248 	MHC Region (ALT_REF_LOCI_6)
GL000256 	NT_167249 	MHC Region (ALT_REF_LOCI_7)
GL000257 	NT_167250 	UGT2B17 Region (ALT_REF_LOCI_8)
GL000258 	NT_167251 	MAPT Region (ALT_REF_LOCI_9)
     */

    private Map<String, String> acc2chrMap;

    public ChromosomeMapper() {
        acc2chrMap = new HashMap<>();
        acc2chrMap.put("CM000663", "chr1");
        acc2chrMap.put("CM000664","chr2");
        acc2chrMap.put("CM000665", "chr3=");
        acc2chrMap.put("CM000666","chr4");
        acc2chrMap.put("CM000667","chr5");
        acc2chrMap.put("CM000668","chr6");
        acc2chrMap.put("CM000669","chr7");
        acc2chrMap.put("CM000670","chr8");
        acc2chrMap.put("CM000671","chr9");

        acc2chrMap.put("CM000672","ch10");
        acc2chrMap.put("CM000673","chr11");
        acc2chrMap.put("CM000674","chr12");
        acc2chrMap.put("CM000675","chr13");
        acc2chrMap.put("CM000676","chr14");
        acc2chrMap.put("CM000677","chr15");
        acc2chrMap.put("CM000678","chr16");
        acc2chrMap.put("CM000679","chr17");
        acc2chrMap.put("CM000680","chr18");
        acc2chrMap.put("CM000681","chr19");
        acc2chrMap.put("CM000682","chr20");
        acc2chrMap.put("CM000683","chr21");
        acc2chrMap.put("CM000684","chr22");
        acc2chrMap.put("CM000685","chrX");
        acc2chrMap.put("CM000686","chrY");

    }

    public Map<String, String> getAcc2chrMap() {
        return acc2chrMap;
    }
}
