.. _rstsetup:

Setting up SvAnna
================

SvAnna is a desktop Java application that requires several external files to run.


Prerequisites
~~~~~~~~~~~~~

SvAnna was written with Java version 11.
`Git <https://git-scm.com/book/en/v2>`_ and
`Java Development Kit <https://www.oracle.com/java/technologies/javase-downloads.html>`_ version 11 or better
are required to build SvAnna from source. Most users should download the prebuilt SvAnna files from the
`Releases page <https://github.com/TheJacksonLaboratory/svann/releases>`_.

Installation
~~~~~~~~~~~~

Go the GitHub page of `SvAnna <https://github.com/TheJacksonLaboratory/svann>`_, and clone or download the project.
Build the executable from source with maven, and then test the build. ::

    $ git clone https://github.com/TheJacksonLaboratory/svann
    $ cd svann
    $ ./mvnw package
    $ java -jar svanna-cli/target/svanna-cli.jar
    $ Usage: <main class> [options] [command] [command options]
      Options:
        -h, --help
          display this help message
      (...)

Prebuilt SvAnna executable
^^^^^^^^^^^^^^^^^^^^^^^^^^

Alternatively, go to the `Releases section <https://github.com/pnrobinson/svann/releases>`_ on the
SvAnna GitHub page and download the latest precompiled version of SvAnna.


The download command
~~~~~~~~~~~~~~~~~~~~

.. _rstdownload:

SvAnna requires four additional files to run. TODO update this list!

1. ``hp.obo``. The main Human Phenotype Ontology file
2. ``phenotype.hpoa`` The main annotation file with all HPO disease models
3. ``Homo_sapiens_gene_info.gz`` A file from NCBI Entrez Gene with information about human genes
4. ``mim2gene_medgen`` A file from the NCBI medgen project with OMIM-derived links between genes and diseases

SvAnna offers a convenience function to download all four files
to a local directory. By default, SvAnna will download all four files into a newly created subdirectory
called ``data`` in the current working directory. You can change this default with the ``-d`` or ``--data`` options
(If you change this, then you will need to pass the location of your directory to all other SvAnna commands
using the ``-d`` flag). Download the files automatically as follows. ::

    $ java -jar svanna-cli.jar download

SvAnna will not download the files if they are already present unless the ``-f`` or ``--force-overwrite`` argument is passed. For
instance, the following command would download the four files to a directory called datafiles and would
overwrite any previously downloaded files. ::

    $ java -jar svanna-cli.jar download -d datafiles --force-overwrite


If desired, you can download these files on your own but you need to place them all in the
same directory to run SvAnna.

DGV: Database of Genomic Variants
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

SvAnna is being extended to filter variants for overlap with population variants. For now, SvAnna uses a downloaded
file from the DGV (http://dgv.tcag.ca/dgv/app/downloads). Choose the file for the correct genome assembly (probably hg38), and
download it. We then need to index this file with `tabix <https://www.ncbi.nlm.nih.gov/pmc/articles/PMC3042176/>`_.
Assuming we have downloaded the file ``GRCh38_hg38_variants_2020-02-25.txt`` from DGV, proceed with the following steps  ::

    $ bgzip GRCh38_hg38_variants_2020-02-25.txt
    $ tabix --skip-lines 1 -s 2 -b 3 -e 4 GRCh38_hg38_variants_2020-02-25.txt.gz

After running the commands, the DGV file is compressed and indexed for quick random access.


