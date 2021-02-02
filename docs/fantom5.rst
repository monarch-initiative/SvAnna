.. _rstfantom5:

FANTOM5
=======

SvAnna uses definitions of transcribed enhancers from
the paper
`An atlas of active enhancers across human cell types and tissues <https://www.nature.com/articles/nature12787>`_.
In detail, SvAnna uses the
``F5.hg38.enhancers.expression.matrix`` file from
https://fantom.gsc.riken.jp/5/datafiles/reprocessed/hg38_latest/extra/enhancer/
and the ``Human.sample_name2library_id.txt`` file from
https://fantom.gsc.riken.jp/5/datafiles/latest/extra/Enhancers/.


How tissue specificity is calculated
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The authors of the above cited paper define facets of related cell type libraries, stating

 The majority of detected enhancers within any facet are not restricted to that facet.
 Exceptions, where facets use a higher fraction of specific enhancers include immune cells,
 neurons, neural stem cells and hepatocytes amongst the cell type facets, and brain, blood,
 liver and testis amongst the organ/tissue facets. Second, despite their apparent promiscuity,
 enhancers are more generally detected in a much smaller subset of samples than mRNA
 transcripts, consistent with cell line studies and the higher specificity of non-coding
 RNAs (ncRNAs) in general. Facets in which we detect many enhancers typically also have
 a higher fraction of facet-specific enhancers. Third, the number of detected expressed
 enhancers and mRNA transcripts is correlated, but the number of
 detected expressed gene transcripts (>1TPM) is 19-34 fold larger than the number of
 detected enhancers with the cutoffs used. Noteworthy exceptions include blood and immune
 cells, testis, thymus, and spleen, which have high enhancer/gene ratio.

Our strategy is to map the libraries to facets (roughly, tissues or groups of tissues) acccording
to the FANTOM5 annotations. We then calculate the tissue-specifity score as detailed below. We then
assign each enhancer to the tissue with the highest tissue specifity (highest number of reads).

TODO -- should we filter out ubiquitous enhancers? Probably yes.



Tissue specificity score
^^^^^^^^^^^^^^^^^^^^^^^^

The specificity score for each robust enhancer was defined to range between 0 and 1,
where 0 means unspecific (ubiquitously expressed across facets) and 1 means specific
(exclusively expressed in one facet).

In detail, specificity(X) = 1 – (entropy(X) / log2(N) ), where X is a vector of
sample-average expression values for an enhancer over all facets (cell types and
organs/tissues were analyzed separately) and N its cardinality (|X|, the number of facets).
