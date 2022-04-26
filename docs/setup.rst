.. _rstsetup:

=================
Setting up SvAnna
=================

SvAnna is a desktop Java application that requires several external files to run. This document explains how to download
the external files and how to prepare SvAnna for running in the local system.

.. note::
  SvAnna is written with Java version 11 and will run and compile under Java 11+.

Installation
^^^^^^^^^^^^

To install SvAnna, you need to get SvAnna distribution ZIP archive that contains the executable JAR file, and SvAnna
database files.


Prebuilt SvAnna executable
~~~~~~~~~~~~~~~~~~~~~~~~~~~

To download the executable SvAnna JAR file, go to the
`Releases section <https://github.com/TheJacksonLaboratory/SvAnna/releases>`_
on the SvAnna GitHub page and download the latest SvAnna ZIP archive.


SvAnna database files
~~~~~~~~~~~~~~~~~~~~~~~~~

SvAnna database files are available for download in the :ref:`rstdownloads` section.

After the download, unzip the archive and put SvAnna database files into a folder of your choice::

  $ unzip -d svanna-data *.svanna.zip


.. note::
  From now on, we will use ``svanna-data`` instead of spelling out the full path to SvAnna database files.
.. tip::
  Keeping the files on a fast hard drive improves the runtime performance.


Build SvAnna from source
~~~~~~~~~~~~~~~~~~~~~~~~

As an alternative to using prebuilt SvAnna JAR file, the SvAnna JAR file can also be built from Java sources.

SvAnna was written with Java version 11.
`Git <https://git-scm.com/book/en/v2>`_ and
`Java Development Kit <https://www.oracle.com/java/technologies/javase-downloads.html>`_ version 11 or better
are required for build.


Run the following commands to download SvAnna source code from GitHub repository and to build SvAnna JAR file::

  $ git clone https://github.com/TheJacksonLaboratory/SvAnna
  $ cd SvAnna
  $ ./mvnw package

After the build, the JAR file is located at ``svanna-cli/target/svanna-cli-${project.version}.jar``::

  $ java -jar svanna-cli/target/svanna-cli-${project.version}.jar --help

.. note::
  From now on, we will use ``svanna-cli.jar`` instead of spelling out the full path to the JAR file within your environment.
