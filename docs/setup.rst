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
  Download `svanna.zip <https://svanna.s3.amazonaws.com/2112_hg38_svanna.zip>`_ (~675 MB for download,  2.4 GB unpacked)

After the download, unzip the archive(s) content into a folder and note the folder path.

.. tip::
  Keeping the files on fast hard drive will improve the runtime performance.


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
    # path to the SvAnna data directory
    dataDirectory:

    #prioritization:
      # term similarity measure, choose from {RESNIK_SYMMETRIC, RESNIK_ASYMMETRIC}
      #termSimilarityMeasure: RESNIK_SYMMETRIC
      # The mode for getting information content of the most informative common ancestors for terms t1, and t2.
      # Choose from {IN_MEMORY, DATABASE}.
      # IN_MEMORY is faster but uses more memory
      # DATABASE is slower but also more memory efficient
      #icMicaMode: DATABASE
      # The transcript promoter region is defined as n bases upstream of the transcription start site
      #promoterLength: 2000
      # Set to 0. to score promoter variants as strictly as coding variants, or to 1. to skip
      #promoterFitnessGain: .6


Mandatory parameters
~~~~~~~~~~~~~~~~~~~~

Open the file in your favorite text editor and provide the following three bits of information:

1. ``dataDirectory`` - location the the folder with SvAnna data. The directory is expected to have a structure like::

    svanna_folder
      |- gencode.v38.genes.json.gz
      |- hp.obo
      \- svanna_db.mv.db

  where ``svanna_folder`` corresponds to content of the ZIP files downloaded in the previous section

.. tip::
  The YAML syntax requires to include a white space between key, value pairs (e.g. ``dataDirectory: /project/joe/svanna_resources``.
