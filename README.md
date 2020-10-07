# lirical2sv
LIRICAL TSV output to list of overlapping Structural Variants.
The executable is called ``lirical2sv`` and can be built using standard
maven tools.


This is a very prototyp-y tool at the moment, caveat emptor

## Running the tool
We plan to make it easier to set things up for the final tool, but here is what we need
to do now to run this app.

### Creating the Jannovar transcript file
[Jannovar](https://github.com/charite/jannovar) is a Java app/library for annotating
VCF files. Its main use case is for small variants and their intersection with
protein coding sequences. We will use it here to extract the positions of genes and
SVs, but it may be easier just to start with a gencode GFF file in the future.

Jannovar downloads various files and creates a transcript file that it uses for VCF annotation.
At present, NCBI etc has changed the location of some files so that only the develop branch
of Jannovar works. Enter the following commands to create the transcript file

```
git clone
https://github.com/charite/jannovar.git
cd jannovar
git checkout develop
mvn package
java [-Xmx8g] -jar jannovar-cli-0.36-SNAPSHOT.jar download -d hg38/refseq_curated 
```
This command downloads various files and generates `data/hg38_refseq_curated.ser`. either move
this to the data subdirectory in this project or softlink it (from 'data', enter `ln -s <path>`).
Thus, for now, this project expects the path `data/data/refseq_curated.ser`.

### LIRICAL
Run [LIRICAL](https://github.com/TheJacksonLaboratory/LIRICAL) using only the clinical manifestations
of the case, and output a TSV file. Consider using the 
[PhenopacketGenerator](https://github.com/TheJacksonLaboratory/PhenopacketGenerator).

## Running lirical2sv

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



