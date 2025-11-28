.. _rstsetup:

#################
Setting up SvAnna
#################

SvAnna is a desktop Java application that requires several external files to run. This document explains how to download
the external files and how to prepare SvAnna for running in the local system.

.. note::
  SvAnna is written with Java version 11 and will run and compile under Java 11+.

To install SvAnna, you need to obtain the code and the database files.
The code is distributed as a ZIP archive or can be built from sources.
The database files include the prebuilt variant databases and a bunch of genotype-phenotype association files
that can be downloaded from internet.

The next sections explain the setup steps in detail.

***********
SvAnna code
***********

Prebuilt SvAnna executable
==========================

To download the executable SvAnna JAR file, go to the
`Releases section <https://github.com/monarch-initiative/SvAnna/releases>`_
on the SvAnna GitHub page and download the latest SvAnna ZIP archive.

The archive includes several files, including a single JAR file named similarly to `svanna-cli-${project.version}.jar`.
The JAR file contains the entire SvAnna codebase.

Verify that the download went well by running::

  $ java -jar svanna-cli-${project.version}.jar --help

The command should print the help message::

  Structural variant prioritization
    Usage: svanna-cli.jar [-hV] [COMMAND]
      -h, --help      Show this help message and exit.
      -V, --version   Print version information and exit.
    Commands:
      setup-phenotype  Setup gene-phenotype resources.
      prioritize       Prioritize the variants.
    See the full documentation at `https://monarch-initiative.github.io/SvAnna/stable`

.. note::

  From now on, we will use ``svanna-cli.jar`` as a placeholder for the full path to the JAR file within your environment.

Build SvAnna from source
========================

As an alternative to using prebuilt SvAnna JAR file, the SvAnna JAR file can also be built from Java sources.

SvAnna was written with Java version 11.
`Git <https://git-scm.com/book/en/v2>`_ and
`Java Development Kit <https://www.oracle.com/java/technologies/javase-downloads.html>`_ version 11 or better
are required for build.


Run the following commands to download SvAnna source code from GitHub repository and to build SvAnna JAR file::

  $ git clone https://github.com/monarch-initiative/SvAnna
  $ cd SvAnna
  $ ./mvnw package

After the build, the JAR file is located at ``svanna-cli/target/svanna-cli-${project.version}.jar``::

  $ java -jar svanna-cli/target/svanna-cli-${project.version}.jar --help

.. note::
  From now on, we will use ``svanna-cli.jar`` instead of spelling out the full path to the JAR file within your environment.


**************
Database files
**************

SvAnna needs the database files to be present in a single directory. A path to the directory is needed by all commands
of SvAnna's command line interface (CLI).


Variant databases
=================

A ZIP archive with SvAnna database files is available for download in the :ref:`rstdownloads` section.
The archive must be downloaded and unzipped into a folder of your choice::

  $ unzip -d svanna-data *.svanna.zip

The command above will unpack the ZIP archive and write the files into ``svanna-data`` folder.
You can name the folder whatever you want but keep the path to the folder around.
We will need it in each SvAnna analysis.

.. note::
  From now on, we will use ``svanna-data`` as a placeholder for the path to the SvAnna data directory.


Genotype-phenotype association files
====================================

SvAnna needs a bunch of files to perform the gene-disease-phenotype matching.
The files can be downloaded with the ``setup-phenotype`` command::

  $ java -jar svanna-cli-.jar setup-phenotype -d svanna-data

The command will download the files and store them at ``svanna-data/phenotype`` folder. The files can be easily updated
by adding ``-w | --overwrite`` option, to overwrite the previously existing files with freshly downloaded data.


Update the genotype-phenotype association files
-----------------------------------------------

The HPO project regularly updates the HPO as well as the HPO annotation database.
Therefore, it is important to update these for SvAnna. The updated files can be downloaded
from the `HPO website <https://hpo.jax.org/>`_ or with SvAnna's ``setup-phenotype`` command.
Given path to ``svanna-data`` and the ``--overwrite`` option, the command will download the most recent files
and store them at the appropriate location.


Data directory structure
========================

The data directory should include the following files::

  $ tree svanna-data
    svanna-data
    ├── checksum.sha256
    ├── gencode.v38.genes.json.gz
    ├── phenotype
    │  ├── hgnc_complete_set.txt
    │  ├── hp.json
    │  ├── mim2gene_medgen
    │  ├── phenotype.hpoa
    │  └── term-pair-similarity.csv.gz
    └── svanna_db.mv.db

