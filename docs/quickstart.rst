.. _rstquickstart:

==========
Quickstart
==========

This document is intended for the impatient users who want to quickly setup and prioritize variants with SvAnna.

Prerequisites
^^^^^^^^^^^^^

SvAnna is written in Java 11 and needs Java 11+ to be present in the runtime environment. Please verify that you are
running Java 11+ by running::

  java -version


SvAnna setup
^^^^^^^^^^^^

Run the following commands to setup SvAnna.

Download and extract SvAnna distribution ZIP archive from `here <https://github.com/TheJacksonLaboratory/SvAnna/releases>`_. Then, run::

  java -jar svanna-cli-1.0.0-RC1/svanna-cli-1.0.0-RC1.jar --help

.. note::
  If things went well, the command above should print the following help message::

    Structural variant annotation
    Usage: svanna-cli.jar [-hV] [COMMAND]
      -h, --help      Show this help message and exit.
      -V, --version   Print version information and exit.
    Commands:
      generate-config, G  Generate a configuration YAML file
      prioritize, P       Prioritize the variants
    See the full documentation at `https://github.com/TheJacksonLaboratory/SvAnna`

Download SvAnna database files::

  svanna_data=TODO
  wget $svanna_data && unzip TODO
  wget https://squirls.s3.amazonaws.com/jannovar_v0.35.zip && unzip jannovar_v0.35.zip


Generate the configuration file::

  java -jar `pwd`/svanna/svanna-cli-1.0.0-RC1.jar generate-config svanna-config.yml

Now use your favorite text editor to fill in the details for:

* ``dataDirectory:`` - the absolute path to the folder where SvAnna database files were extracted
* ``jannovarCachePath`` - the absolute path to selected Jannovar ``*.ser`` file, e.g. ``/path/to/hg38_refseq.ser``

.. tip::
  YAML syntax requires a whitespace to be present between the *key*: *value* pairs.

Prioritize structural variants in VCF file
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Having completed the steps above, you're good to prioritize variants in VCF file. Let's annotate a toy VCF file containing
eight variants reported in the SvAnna manuscript. First, let's download the VCF file::

  # download the VCF file
  wget https://github.com/TheJacksonLaboratory/SvAnna/blob/master/svanna-cli/src/examples/example.vcf

Now, let's prioritize the variants::

  java -jar `pwd`/svanna/svanna-cli-1.0.0-RC1.jar prioritize svanna-config.yml --output-format html,csv,vcf --term HP:123456 --vcf example.vcf

SvAnna will annotate the VCF file and store the results next to the VCF file.


