.. _rstsetup:

=================
Setting up SvAnna
=================

SvAnna is a desktop Java application that requires several external files to run. This document explains how to download
the external files and how to prepare SvAnna for running in the local system.

.. note::
  SvAnna is written with Java version 11 and will run and compile under Java 11+.

Prerequisites
^^^^^^^^^^^^^

SvAnna was written with Java version 11.
`Git <https://git-scm.com/book/en/v2>`_ and
`Java Development Kit <https://www.oracle.com/java/technologies/javase-downloads.html>`_ version 11 or better
are required to build SvAnna from source.

Installation
^^^^^^^^^^^^

To install SvAnna, you need to get SvAnna distribution ZIP archive that contains the executable JAR file, and SvAnna
database files.

Prebuilt SvAnna executable
~~~~~~~~~~~~~~~~~~~~~~~~~~~

To download the prebuilt SvAnna JAR file, go to the
`Releases section <https://github.com/TheJacksonLaboratory/SvAnna/releases>`_
on the SvAnna GitHub page and download the latest precompiled version of SvAnna.

SvAnna database files
~~~~~~~~~~~~~~~~~~~~~~~~~

SvAnna database files are available for download from:

**hg38/GRCh38**
  Download `svanna.zip <https://svanna.s3.amazonaws.com/svanna.zip>`_ (~600 MB for download,  2.6 GB unpacked)

After the download, unzip the archive(s) content into a folder and note the folder path.

.. tip::
  Keeping the files on fast hard drive will improve the runtime performance.

Jannovar transcript databases
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Functional annotation of variants, which is required for certain SvAnna tasks, is performed using `Jannovar`_ library.
To run the annotation, Jannovar transcript database files need to be provided. The Jannovar ``v0.35`` database files were
tested to work with SvAnna.

For your convenience, the files containing *UCSC*, *RefSeq*, or *ENSEMBL* transcripts
for *hg19* or *hg38* genome assemblies are available for download (~330 MB for download, ~330 MB unpacked).

Download Jannovar files from ftp://squirls.ielis.xyz/jannovar_v0.35.zip::

  $ wget ftp://squirls.ielis.xyz/jannovar_v0.35.zip
  or
  $ curl --output jannovar_v0.35.zip ftp://squirls.ielis.xyz/jannovar_v0.35.zip


Build SvAnna from source
~~~~~~~~~~~~~~~~~~~~~~~~

As an alternative to using prebuilt SvAnna JAR file, the SvAnna JAR file can also be built from Java sources.

Run the following commands to download SvAnna source code from GitHub repository and to build SvAnna JAR file::

  $ git https://github.com/TheJacksonLaboratory/SvAnna
  $ cd SvAnna
  $ ./mvnw package

.. note::
  To build SvAnna from sources, JDK 11 or better must be available in the environment

After the successful build, the JAR file is located at ``svanna-cli/target/svanna-cli-${project.version}.jar``.

To verify that the building process went well, run::

  $ java -jar svanna-cli/target/svanna-cli-${project.version}.jar --help

You should see a message describing how to run the individual commands.

.. note::
  From now on, we will use ``svanna-cli.jar`` instead of spelling out the full path to the JAR file within your environment.

.. _generate-config-ref:

``generate-config`` - Generate and fill the configuration file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

SvAnna needs to know about the locations of the external files. The locations are provided in a YAML configuration file.
The command ``generate-config`` generates an empty configuration file::

  $ java -jar svanna-cli.jar generate-config svanna-config.yml


The command above generates an empty configuration file ``svanna-config.yml`` in the working directory.

The configuration file has the following content::

  # Required properties template, the file follows YAML syntax.
  svanna:
  # path to folder with SvAnna database and the other required files
  dataDirectory:
  # path to Jannovar transcript database
  jannovarCachePath:

  #dataParameters:
    # TAD must be present in at least 90% of the tissues to be included in the analysis
    #tadStabilityThreshold: 90
    #enhancers:
      # Include VISTA developmental enhancers in the analysis
      #useVista: true
  #prioritizationParameters:
    # Evaluate effect of all variants (including single-gene events) in context of the TAD domain
    #forceTadEvaluation: false
    # term similarity measure, choose from {RESNIK_SYMMETRIC, RESNIK_ASYMMETRIC}
    #termSimilarityMeasure: RESNIK_SYMMETRIC
    # The mode for getting information content of the most informative common ancestors for terms t1, and t2.
    # Choose from {IN_MEMORY, DATABASE}.
    # IN_MEMORY is faster but uses more memory
    # DATABASE is slower but also more memory efficient
    #icMicaMode: DATABASE
    # An event involving max N genes to be considered by the prototype prioritizer
    #maxGenes: 100
    # Number of bases prepended to a transcript and evaluated as a promoter region
    #promoterLength: 2000
    # Set to 0. to score promoter variants as strictly as coding variants, or to 1. to skip
    #promoterFitnessGain: .6

Mandatory parameters
~~~~~~~~~~~~~~~~~~~~

Open the file in your favorite text editor and provide the following three bits of information:

1. ``dataDirectory`` - location the the folder with SvAnna data. The directory is expected to have a structure like::

    svanna_folder
      |- svanna_db.mv.db
      |- hp.obo
      |- phenotype.hpoa
      |- mim2gene_medgen
      \- Homo_sapiens.gene_info.gz

  where ``svanna_folder`` corresponds to content of the ZIP files downloaded in the previous section

2. ``jannovarCachePath`` - path to Jannovar transcript database to be used for analysis.

.. tip::
  The YAML syntax requires to include a white space between key, value pairs (e.g. ``dataDirectory: /project/joe/svanna_resources``.

.. _Jannovar: https://pubmed.ncbi.nlm.nih.gov/24677618