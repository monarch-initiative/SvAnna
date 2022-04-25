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

SvAnna database files are available for download in the :ref:`rstdownloads` section.

After the download, unzip the archive(s) content into a folder of your choice and note down the path.

.. tip::
  Keeping the files on a fast hard drive will improve the runtime performance.


Build SvAnna from source
~~~~~~~~~~~~~~~~~~~~~~~~

As an alternative to using prebuilt SvAnna JAR file, the SvAnna JAR file can also be built from Java sources.

Run the following commands to download SvAnna source code from GitHub repository and to build SvAnna JAR file::

  $ git clone https://github.com/TheJacksonLaboratory/SvAnna
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
