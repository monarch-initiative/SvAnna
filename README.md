# SvAnna - Structural Variant Annotation and Analysis

[![GitHub release](https://img.shields.io/github/release/TheJacksonLaboratory/SvAnna.svg)](https://github.com/TheJacksonLaboratory/SvAnna/releases)
[![Java CI with Maven](https://github.com/TheJacksonLaboratory/SvAnna/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/TheJacksonLaboratory/SvAnna/actions/workflows/maven.yml)
[![Documentation Status](https://readthedocs.org/projects/svanna/badge/?version=master)](https://svanna.readthedocs.io/en/master/?badge=master)

Efficient and accurate pathogenicity prediction for coding and regulatory structural variants in long-read genome sequencing.

Most users should download the latest SvAnna distribution ZIP file from
the [Releases page](https://github.com/TheJacksonLaboratory/SvAnna/releases).

## Example use

SvAnna is a standalone command-line Java application and can be run as follows:

```shell
java -jar svanna-cli.jar -d path/to/svanna/data \
  -t HP:0008330 \
  --vcf example.vcf.gz \
  --output-format html,csv,vcf
```

The analysis will filter out common SVs and perform phenotype-driven prioritization of the remaining SVs. 
The SVs are assigned with *"Pathogenicity of Structural variation"* (PSV) score and written into 
one of several output formats, such as CSV table, a VCF file, or a detailed HTML report.

### HTML report

The HTML report includes a header with the analysis summary and the SVs ordered by the PSV score 
with the best scores on top.

### Analysis summary

The summary presents the clinical features encoded into terms of Human Phenotype Ontology (HPO) as well as 
the other analysis parameters.

![Analysis summary](img/analysis-summary.png)

### Variant counts

The report further breaks down SVs into several categories:

![Variant counts](img/variant-counts.png)

### Structural variants

Last, each SV is presented in the context of the overlapping genes and transcripts:
![Variant transcript summary](img/variant-tx-summary.png)

We also show the variant in context of the neighboring repetitive regions and genes/transcripts:
![Variant context](img/variant-tx-context.png)


## Read more

Please consult the Read the docs site for a detailed documentation:
- [stable version](https://svanna.readthedocs.io/en/master) describing the latest release at the *Releases page*, or
- [latest version](https://svanna.readthedocs.io/en/latest) summarizing the latest development on `development` branch.

Check out SvAnna manuscript in [Genome Medicine](https://doi.org/10.1186/s13073-022-01046-6).
