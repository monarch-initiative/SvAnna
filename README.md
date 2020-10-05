# lirical2overlappingsv
LIRICAL TSV output to list of overlapping Structural Variants.
The executable is called ``l2o`` and can be built using standard
maven tools.



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


