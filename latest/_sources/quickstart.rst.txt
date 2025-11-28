.. _rstquickstart:

========
Tutorial
========

This document is an end-to-end tutorial for the impatient users
who want to quickly setup and prioritize structural variants with SvAnna.

Prerequisites
^^^^^^^^^^^^^

SvAnna is written in Java 11 and needs Java 11+ to be present in the runtime environment. Please verify that you are
using Java 11+ by running::

  $ java -version

If ``java`` is present on your ``$PATH``, then the command above will print a message similar to this one::

  openjdk version "11" 2018-09-25
  OpenJDK Runtime Environment 18.9 (build 11+28)
  OpenJDK 64-Bit Server VM 18.9 (build 11+28, mixed mode)

Setup
^^^^^

SvAnna is installed by running the following three steps.

1. Download SvAnna distribution ZIP
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Download and extract SvAnna distribution ZIP archive from `GitHub releases <https://github.com/monarch-initiative/SvAnna/releases>`_.
Expand the *Assets* menu and download the ``svanna-cli-${project.version}-distribution.zip``. Choose the latest stable version,
or a release candidate (RC).

After unzipping the distribution archive, run the following command to display the help message::

  $ java -jar svanna-cli-${project.version}.jar --help

.. note::
  If things went well, the command above will print the following help message::

    Structural variant prioritization
    Usage: svanna-cli.jar [-hV] [COMMAND]
      -h, --help      Show this help message and exit.
      -V, --version   Print version information and exit.
    Commands:
      setup-phenotype  Setup gene-phenotype resources.
      prioritize       Prioritize the variants.
    See the full documentation at `https://monarch-initiative.github.io/SvAnna/stable`


2. Download SvAnna database files
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

SvAnna database files are available for download in the :ref:`rstdownloads` section.

After the download, unzip the archive(s) content into a folder of your choice and note down the path::

  $ unzip -d svanna-data *.svanna.zip

The command extracts the archive content into a new folder called ``svanna-data``
We will need the data folder path in the next steps.


3. Setup the genotype-phenotype resources
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

SvAnna needs additional data files from the Human Phenotype Ontology (HPO) project to support the gene-phenotype matching.
These files can be downloaded with the ``setup-phenotype`` command:

  $ java -jar svanna-cli.jar setup-phenotype -d svanna-data

The command asks for a path to SvAnna data directory (defined in the previous step) and will download the files,
precompute information content for HPO term pairs, and store the files into ``phenotype`` subfolder (e.g. ``svanna-data/phenotype``).


Prioritize structural variants in VCF file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Now, let's annotate a toy VCF file containing eight SVs reported in the SvAnna manuscript.
First, let's download the VCF file from
`SvAnna source code repository <https://github.com/monarch-initiative/SvAnna/blob/master/svanna-cli/src/examples/example.vcf>`_::

  $ wget https://raw.githubusercontent.com/monarch-initiative/SvAnna/master/svanna-cli/src/examples/example.vcf

The variants were sourced from published clinical case reports and presence of each variant results in a Mendelian disease.

For the purpose of this test run, let's assume that the VCF file contains SVs identified in a short/long read
sequencing run of a patient presenting with the following clinical symptoms:

* *HP:0011890* - Prolonged bleeding following procedure
* *HP:0000978* - Bruising susceptibility
* *HP:0012147* - Reduced quantity of Von Willebrand factor

Now, let's prioritize the variants::

  $ java -jar svanna-cli-${project.version}.jar prioritize \
    -d svanna-data \
    --output-format html,csv,vcf \
    --vcf example.vcf \
    --phenotype-term HP:0011890 \
    --phenotype-term HP:0000978 \
    --phenotype-term HP:0012147


The variant ``Othman-2010-20696945-VWF-index-FigS7`` disrupts a promoter of the *von Willenbrand factor*
(*VWF*) gene (`Othman et al., 2010 <https://pubmed.ncbi.nlm.nih.gov/20696945>`_).
The variant receives the highest :math:`PSV` score of 47.26, and it is ranked first.

SvAnna stores prioritization results in *HTML*, *CSV*, and *VCF* output formats in the current working directory.
