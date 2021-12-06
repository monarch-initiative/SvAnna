.. _rstexamples:

========
Examples
========

This section shows how SvAnna prioritizes various structural variant classes.
The resulting HTML reports contain graphics that are reported in the supplement of SvAnna paper.

The examples work with variants stored in `examples.vcf`_ file. The VCF file is stored in SvAnna GitHub repository.

Single exon deletion
^^^^^^^^^^^^^^^^^^^^

A deletion of 6.93 kb (``chr17:31,150,798-31,157,725del``) affecting *NF1* that was assigned a *PSV* score of 157.95.

The deletion affects exon 2 of several *NF1* transcripts.
Pathogenic variants in *NF1* are associated with neurofibromatosis type 1 (``OMIM:162200``).

The phenotypic features curated for the proband ``UAB-1`` were:

* ``HP:0007565`` Multiple cafe-au-lait spots
* ``HP:0009732`` Plexiform neurofibroma
* ``HP:0009735`` Spinal neurofibromas
* ``HP:0009736`` Tibial pseudarthrosis

Data were curated from a published case report in `Decoding NF1 Intragenic Copy-Number Variations`_.

Command
~~~~~~~

.. code-block:: console

    $ java -jar svanna-cli.jar prioritize -c svanna-config.yml --vcf example.vcf --term HP:0007565 --term HP:0009732 --term HP:0009735 --term HP:0009736



Deletion of multiple exons
^^^^^^^^^^^^^^^^^^^^^^^^^^

A deletion of 10.26 kb (``chr17:43,100,079-43,110,335del``) affecting *BRCA1* that was assigned a *PSV* score of 427.00.

The deletion affects three *BRCA1* exons. Pathogenic variants in *BRCA1* are associated with
Breast-ovarian cancer, familial, 1 (``OMIM:604370``).

The phenotypic feature curated for this case was:

* ``HP:0003002`` Breast carcinoma

Data were curated from a published case report `The first case report of a large deletion of the BRCA1 gene in Croatia`_.

Command
~~~~~~~

.. code-block:: console

    $ java -jar svanna-cli.jar prioritize -c svanna-config.yml --vcf example.vcf --term HP:0003002



Deletion of multiple genes
^^^^^^^^^^^^^^^^^^^^^^^^^^

Deletion of 481.73 kb (``chr2:109,923,337-110,405,062del``) affecting *MTLN*, *MALL*, *MTLN*, and *NPHP1*
that was assigned a *PSV* score of 17.60.

Pathogenic variants in *NPHP1* are associated with Joubert syndrome 4 (``OMIM:609583``).

The phenotypic features curated for this case were:

* ``HP:0003774`` Stage 5 chronic kidney disease
* ``HP:0001320`` Cerebellar vermis hypoplasia
* ``HP:0002078`` Truncal ataxia
* ``HP:0000618`` Blindness
* ``HP:0000508`` Ptosis
* ``HP:0002419`` Molar tooth sign on MRI
* ``HP:0011933`` Elongated superior cerebellar peduncle
* ``HP:0002070`` Limb ataxia
* ``HP:0000543`` Optic disc pallor
* ``HP:0000589`` Coloboma

Data were curated from a published case report `Whole-exome sequencing and digital PCR identified a novel compound heterozygous mutation in the NPHP1 gene in a case of Joubert syndrome and related disorders`_.

Command
~~~~~~~

.. code-block:: console

    $ java -jar svanna-cli.jar prioritize -c svanna-config.yml --vcf example.vcf --term HP:0003774 --term HP:0001320 --term HP:0002078 --term HP:0000618 --term HP:0000508 --term HP:0002419 --term HP:0011933 --term HP:0002070 --term HP:0000543 --term HP:0000589



Duplication of coding sequence
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Duplication of 36 bp (``chr13:72835296-72835332dup``) affecting *PIBF1* that was assigned a *PSV* score of 3.38.
Pathogenic variants in *PIBF1* are associated with Joubert syndrome 33 (``OMIM:617767``).

The phenotypic features curated for this case were:

* ``HP:0032417`` Periglomerular fibrosis
* ``HP:0000076`` Vesicoureteral reflux
* ``HP:0002079`` Hypoplasia of the corpus callosum
* ``HP:0001541`` Ascites
* ``HP:0000540`` Hypermetropia
* ``HP:0011968`` Feeding difficulties
* ``HP:0001250`` Seizure
* ``HP:0000490`` Deeply set eye
* ``HP:0001263`` Global developmental delay
* ``HP:0001284`` Areflexia
* ``HP:0002240`` Hepatomegaly
* ``HP:0001290`` Generalized hypotonia
* ``HP:0031200`` Hyaline casts
* ``HP:0011800`` Midface retrusion
* ``HP:0000090`` Nephronophthisis
* ``HP:0000092`` Renal tubular atrophy
* ``HP:0001919`` Acute kidney injury
* ``HP:0012650`` Perisylvian polymicrogyria
* ``HP:0002419`` Molar tooth sign on MRI
* ``HP:0002119`` Ventriculomegaly
* ``HP:0000105`` Enlarged kidney

Data were curated from a published case report `A biallelic 36-bp insertion in PIBF1 is associated with Joubert syndrome`_

Command
~~~~~~~

.. code-block:: console

    $ java -jar svanna-cli.jar prioritize -c svanna-config.yml --vcf example.vcf --term HP:0032417 --term HP:0000076 --term HP:0002079 --term HP:0001541 --term HP:0000540 --term HP:0011968 --term HP:0001250 --term HP:0000490 --term HP:0001263 --term HP:0001284 --term HP:0002240 --term HP:0001290 --term HP:0031200 --term HP:0011800 --term HP:0000090 --term HP:0000092 --term HP:0001919 --term HP:0012650 --term HP:0002419 --term HP:0002119 --term HP:0000105



Multigene inversion
^^^^^^^^^^^^^^^^^^^

Inversion of ~12.23 kb (``inv(chr3)(9725702; 9737931)``) that disrupts the coding sequence of *BRPF1* was assigned
*PSV* score of 9.25.

Pathogenic variants in *BRPF1* are associated with Intellectual developmental disorder with dysmorphic facies and ptosis ``OMIM:617333``.

The phenotypic features curated for this case were:

* ``HP:0000316`` Hypertelorism
* ``HP:0000494`` Downslanted palpebral fissures
* ``HP:0000431`` Wide nasal bridge
* ``HP:0000286`` Epicanthus
* ``HP:0000311`` Round face
* ``HP:0012368`` Flat face
* ``HP:0000486`` Strabismus
* ``HP:0000508`` Ptosis
* ``HP:0002949`` Fused cervical vertebrae
* ``HP:0002194`` Delayed gross motor development
* ``HP:0000750`` Delayed speech and language development
* ``HP:0002342`` Intellectual disability, moderate
* ``HP:0011150`` Myoclonic absence seizure
* ``HP:0002069`` Bilateral tonic-clonic seizure
* ``HP:0001252`` Hypotonia


Data were curated from a published case report `Pathogenic 12-kb copy-neutral inversion in syndromic intellectual disability identified by high-fidelity long-read sequencing`_

Command
~~~~~~~

.. code-block:: console

    $ java -jar svanna-cli.jar prioritize -c svanna-config.yml --vcf example.vcf --term HP:0000286 --term HP:0002069 --term HP:0000494 --term HP:0002342 --term HP:0000486 --term HP:0000750 --term HP:0000431 --term HP:0001252 --term HP:0002194 --term HP:0012368 --term HP:0011150 --term HP:0002949 --term HP:0000508 --term HP:0000316 --term HP:0000311



Deletion affecting transcription start site
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Deletion of ∼1.57 kb (``chrX:64,205,190-64,206,761del``) affecting transcription start site of *AMER1* was assigned
*PSV* score of 10.39.

Pathogenic variants in *AMER1* are associated with Osteopathia striata with cranial sclerosis (``OMIM:300373``).

The phenotypic features curated for this case were:

* ``HP:0001561`` Polyhydramnios
* ``HP:0002684`` Thickened calvaria
* ``HP:0000256`` Macrocephaly
* ``HP:0000316`` Hypertelorism
* ``HP:0031367`` Metaphyseal striations
* ``HP:0002744`` Bilateral cleft lip and palate
* ``HP:0002781`` Upper airway obstruction
* ``HP:0001004`` Lymphedema
* ``HP:0000750`` Delayed speech and language development

Data were curated from a published case report `Deletion of Exon 1 in AMER1 in Osteopathia Striata with Cranial Sclerosis`_.

Command
~~~~~~~

.. code-block:: console

    $ java -jar svanna-cli.jar prioritize -c svanna-config.yml --vcf example.vcf --term HP:0001561 --term HP:0000750 --term HP:0002684 --term HP:0002781 --term HP:0000316 --term HP:0031367 --term HP:0002744 --term HP:0000256 --term HP:0001004



Deletion affecting promoter region
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A deletion of 13 bp (``chr12:6,124,705-6,124,718del``) located in the core promoter region of *VWF* was assigned *PSV* score of
64.18.

In the original publication, the deletion was shown to lead to aberrant binding of Ets transcription factors to the site
of the deletion (30 bp upstream of *ENST00000261405.10*) and thereby reduce VWF expression.

Pathogenic variants in *VWF* are associated with von Willebrand disease (``OMIM:193400``).

The phenotypic features curated for this case were:

* ``HP:0011890`` Prolonged bleeding following procedure
* ``HP:0000978`` Bruising susceptibility
* ``HP:0012147`` Reduced quantity of Von Willebrand factor

Data were curated from a published case report `Functional characterization of a 13-bp deletion (c.-1522_-1510del13) in the promoter of the von Willebrand factor gene in type 1 von Willebrand disease`_.

Command
~~~~~~~

.. code-block:: console

    $ java -jar svanna-cli.jar prioritize -c svanna-config.yml --vcf example.vcf --term HP:0011890 --term HP:0000978 --term HP:0012147



Translocation disrupting a gene sequence
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A translocation (``t(chr3:11,007,014; chr4:139,383,334)``) affecting *SLC6A1* was assigned *PSV* score of 4.74.

Pathogenic variants in *SLC6A1* are associated with Myoclonic-atonic epilepsy (``OMIM:616421``).

The phenotypic features curated for this case were:

* ``HP:0000252`` Microcephaly
* ``HP:0000446`` Narrow nasal bridge
* ``HP:0000272`` Malar flattening
* ``HP:0000219`` Thin upper lip vermilion
* ``HP:0000179`` Thick lower lip vermilion
* ``HP:0002650`` Scoliosis
* ``HP:0002987`` Elbow flexion contracture
* ``HP:0006380`` Knee flexion contracture
* ``HP:0001250`` Seizure
* ``HP:0001263`` Global developmental delay
* ``HP:0001276`` Hypertonia


Data were curated from a published case report `Phenotypic consequences of gene disruption by a balanced de novo translocation involving SLC6A1 and NAA15`_

Command
~~~~~~~

.. code-block:: console

    $ java -jar svanna-cli.jar prioritize -c svanna-config.yml --vcf example.vcf --term HP:0000252 --term HP:0000446 --term HP:0000272 --term HP:0000219 --term HP:0000179 --term HP:0002650 --term HP:0002987 --term HP:0006380 --term HP:0001250 --term HP:0001263 --term HP:0001263 --term HP:0001276



.. _examples.vcf: https://github.com/TheJacksonLaboratory/Squirls/blob/development/squirls-cli/src/examples/example.vcf
.. _Decoding NF1 Intragenic Copy-Number Variations: https://pubmed.ncbi.nlm.nih.gov/26189818
.. _The first case report of a large deletion of the BRCA1 gene in Croatia: https://pubmed.ncbi.nlm.nih.gov/29310340
.. _Whole-exome sequencing and digital PCR identified a novel compound heterozygous mutation in the NPHP1 gene in a case of Joubert syndrome and related disorders: https://pubmed.ncbi.nlm.nih.gov/28347285
.. _A biallelic 36-bp insertion in PIBF1 is associated with Joubert syndrome: https://pubmed.ncbi.nlm.nih.gov/29695797
.. _Pathogenic 12-kb copy-neutral inversion in syndromic intellectual disability identified by high-fidelity long-read sequencing: https://pubmed.ncbi.nlm.nih.gov/33157260
.. _Deletion of Exon 1 in AMER1 in Osteopathia Striata with Cranial Sclerosis: https://pubmed.ncbi.nlm.nih.gov/33265914
.. _Functional characterization of a 13-bp deletion (c.-1522_-1510del13) in the promoter of the von Willebrand factor gene in type 1 von Willebrand disease: https://pubmed.ncbi.nlm.nih.gov/20696945
.. _Phenotypic consequences of gene disruption by a balanced de novo translocation involving SLC6A1 and NAA15: https://pubmed.ncbi.nlm.nih.gov/29621621
