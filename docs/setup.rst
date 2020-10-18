.. _rstsetup:

Setting up svann
================

svann is a desktop Java application that requires several external files to run.


Prerequisites
~~~~~~~~~~~~~

svann was written with Java version 11. If you want to
build svann from source, then the build process described below requires
`Git <https://git-scm.com/book/en/v2>`_ and `maven <https://maven.apache.org/install.html>`_.

Installation
~~~~~~~~~~~~

Go the GitHub page of `svann <https://github.com/pnrobinson/svann>`_, and clone or download the project.
Build the executable from source with maven, and then test the build. ::

    $ git clone https://github.com/pnrobinson/svann
    $ cd svann
    $ mvn package
    $ java -jar target/svann.jar
    $ Usage: <main class> [options] [command] [command options]
      Options:
        -h, --help
          display this help message
      (...)



svann requires `maven <https://maven.apache.org/>`_ version 3.5.3.


Prebuilt svann executable
^^^^^^^^^^^^^^^^^^^^^^^^^^^

Alternatively, go to the `Releases section <https://github.com/pnrobinson/svann/releases>`_ on the
svann GitHub page and download the latest precompiled version of svann.


The download command
~~~~~~~~~~~~~~~~~~~~

.. _rstdownload:

svann requires four additional files to run. TODO update this list!

1. ``hp.obo``. The main Human Phenotype Ontology file
2. ``phenotype.hpoa`` The main annotation file with all HPO disease models
3. ``Homo_sapiens_gene_info.gz`` A file from NCBI Entrez Gene with information about human genes
4. ``mim2gene_medgen`` A file from the NCBI medgen project with OMIM-derived links between genes and diseases

svann offers a convenience function to download all four files
to a local directory. By default, svann will download all four files into a newly created subdirectory
called ``data`` in the current working directory. You can change this default with the ``-d`` or ``--data`` options
(If you change this, then you will need to pass the location of your directory to all other svann commands
using the ``-d`` flag). Download the files automatically as follows. ::

    $ java -jar svann.jar download

svann will not download the files if they are already present unless the ``--overwrite`` argument is passed. For
instance, the following command would download the four files to a directory called datafiles and would
overwrite any previously downloaded files. ::

    $ java -jar svann.jar download -d datafiles --overwrite


If desired, you can download these files on your own but you need to place them all in the
same directory to run svann.

