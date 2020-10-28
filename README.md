# svann

This is a tool to provide annotations for structural variants in VCF files.

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

## Running svann

Enter the following command to see options. The LIRICAL file is the 
LIRICAL TSV output file. The enhancers file is created by the
https://github.com/pnrobinson/tspec app. To use the enhancers file
it is required to also use an HPO term with the major phenotypic abnormality, 
e.g., [Abnormality of the immune system](https://hpo.jax.org/app/browse/term/HP:0002715).

```
$  java -jar target/svann.jar annotate -h
  Usage: svann annotate [-hV] [-e=<enhancerFile>] [-g=<geneCodePath>]
                        [-j=<jannovarPath>] [-t=<hpoTermIdList>] -v=<vcfFile>
                        [-x=<outprefix>]
  annotate VCF file
    -e, --enhancer=<enhancerFile>
                               tspec enhancer file
    -g, --gencode=<geneCodePath>
  
    -h, --help                 Show this help message and exit.
    -j, --jannovar=<jannovarPath>
                               prefix for output files (default:
                                 data/data/hg38_refseq_curated.ser )
    -t, --term=<hpoTermIdList> HPO term IDs (comma-separated list)
    -v, --vcf=<vcfFile>
    -V, --version              Print version information and exit.
    -x, --prefix=<outprefix>   prefix for output files (default: L2O )
```




# Documentation

Generate the read the docs documentation locally by going to the ``docs`` subdirectory.
First generate a virtual environment and install the required sphinx packages. ::

    virtualenv p38
    source p38/bin/activate
    pip install sphinx sphinx-rtd-theme
    
To create the documentation, ensure you are using the ``p38`` environment and enter the following command. ::

    source p38/bin/activate
    make html
    
This will generate HTML pages under ``_build/html``.