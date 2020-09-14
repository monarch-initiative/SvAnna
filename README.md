# lirical2overlappingsv
LIRICAL TSV output to list of overlapping Structural Variants

This is a very prototyp-y tool at the moment, caveat emptor

## Running the tool

1. First run Jannovar on a VCF file produced by a long-read variant caller, e.g., 
```
$ java -jar jannovar-cli-0.36-SNAPSHOT.jar annotate-vcf -d data/hg38_refseq.ser -i example_hg38_minimap2_pbsv.vcf -o example_minimap_pbsv.vcf
```
2. The run LIRICAL on the corresponding phenopacket


3. Finally, run this tool
