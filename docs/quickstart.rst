.. _rstquickstart:

==========
Quickstart
==========

This document is intended for the impatient users who want to quickly setup and prioritize variants with SvAnna.

Prerequisites
^^^^^^^^^^^^^

SvAnna is written in Java 11 and needs Java 11+ to be present in the runtime environment. Please verify that you are
using Java 11+ by running::

  $ java -version

If ``java`` is present on your ``$PATH``, then the command above will print a message similar to this one::

  openjdk version "11" 2018-09-25
  OpenJDK Runtime Environment 18.9 (build 11+28)
  OpenJDK 64-Bit Server VM 18.9 (build 11+28, mixed mode)

SvAnna setup
^^^^^^^^^^^^

SvAnna is installed by running the following three steps.

1. Download SvAnna distribution ZIP
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Download and extract SvAnna distribution ZIP archive from `here <https://github.com/TheJacksonLaboratory/SvAnna/releases>`_.
Expand the *Assets* menu and download the ``svanna-cli-${project.version}-distribution.zip``. Choose the latest stable version,
or a release candidate (RC).

After unzipping the distribution archive, run the following command to display the help message::

  $ java -jar svanna-cli-${project.version}.jar --help

.. note::
  If things went OK, the command above will print the following help message::

    Structural variant prioritization
    Usage: svanna-cli.jar [-hV] [COMMAND]
      -h, --help      Show this help message and exit.
      -V, --version   Print version information and exit.
    Commands:
      generate-config, G  Generate a configuration YAML file
      prioritize, P       Prioritize the variants
    See the full documentation at `https://github.com/TheJacksonLaboratory/SvAnna`

2. Download SvAnna database files
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Download SvAnna data files by running::

  $ wget https://svanna.s3.amazonaws.com/2112_hg38_svanna.zip

.. tip::
  Use ``curl --output 2112_hg38_svanna.zip https://svanna.s3.amazonaws.com/2112_hg38_svanna.zip`` if ``wget`` is not available in your environment

3. Generate & fill the configuration file
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Generate the configuration file::

  $ java -jar `pwd`/svanna/svanna-cli-${project.version}.jar generate-config svanna-config.yml

Now open the generated file in your favorite text editor and provide absolute paths to the SvAnna database files:

* ``dataDirectory:`` - the absolute path to the folder where SvAnna database files were extracted

.. tip::
  The YAML syntax requires a whitespace to be present between the *key*: *value* pairs.

Note the location of the configuration file, as the path to the configuration file must be provided for all SvAnna runs.
Having completed the steps above, you are good to prioritize variants in a VCF file.

Prioritize structural variants in VCF file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Let's annotate a toy VCF file containing eight SVs reported in the SvAnna manuscript.

First, let's download the VCF file::

  $ wget https://github.com/TheJacksonLaboratory/SvAnna/blob/master/svanna-cli/src/examples/example.vcf

The variants were sourced from published clinical case reports and each variant led to a Mendelian disease.

For the purpose of this test run, let's assume that the VCF file contains SVs identified in a short/long read
sequencing run of a patient presenting with the following clinical symptoms:

* *HP:0011890* - Prolonged bleeding following procedure
* *HP:0000978* - Bruising susceptibility
* *HP:0012147* - Reduced quantity of Von Willebrand factor

Now, let's prioritize the variants::

  $ java -jar svanna/svanna-cli-${project.version}.jar prioritize --config svanna-config.yml --output-format html,csv,vcf --vcf example.vcf --term HP:0011890 --term HP:0000978 --term HP:0012147

The variant with ID ``Othman-2010-20696945-VWF-index-FigS7`` that disrupts a promoter of the *von Willenbrand factor*
(*VWF*) gene (`Othman et al., 2010 <https://pubmed.ncbi.nlm.nih.gov/20696945>`_)
receives the highest :math:`PSV` score of 64.18, and the variant is ranked first.

SvAnna stores prioritization results in *HTML*, *CSV*, and *VCF* output formats next to the input VCF file.

Read the :ref:`rstsetup` and :ref:`rstrunning` sections to learn all details regarding setting up and running SvAnna.
