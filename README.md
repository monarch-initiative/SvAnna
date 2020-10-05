# lirical2overlappingsv
LIRICAL TSV output to list of overlapping Structural Variants.
The executable is called ``l2o`` and can be built using standard
maven tools.


This is a very prototyp-y tool at the moment, caveat emptor

## Running the tool

1. First run Jannovar on a VCF file produced by a long-read variant caller, e.g., 
```
$ java -jar jannovar-cli-0.36-SNAPSHOT.jar annotate-vcf -d data/hg38_refseq.ser -i example_hg38_minimap2_pbsv.vcf -o example_minimap_pbsv.vcf
```
2. The run LIRICAL on the corresponding phenopacket


3. Finally, run this tool



## Running l2o

Enter the following command to see options. The LIRICAL file is the 
LIRICAL TSV output file. The enhancers file is created by the
https://github.com/pnrobinson/tspec app. To use the enhancers file
it is required to also use an HPO term with the major phenotypic abnormality, 
e.g., [Abnormality of the immune system](https://hpo.jax.org/app/browse/term/HP:0002715).

```
$ java -jar target/l2o.jar
LIRICAL to overlapping SV
  -h, --help            Show this help message and exit.
  -l, --lirical=<liricalFile>

  -o, --out=<outname>
  -v, --vcf=<vcfFile>
  -V, --version         Print version information and exit.

```


We can run l2o to look for specific enhancer overlaps, e.g., for terms with
	[Abnormal T cell morphology - HP:0002843](https://hpo.jax.org/app/browse/term/HP:0002843).



