truncate table SVANNA.ENHANCERS;

insert into SVANNA.ENHANCERS(ENHANCER_ID, CONTIG, START_POS, END_POS, ENHANCER_SOURCE, NAME, IS_DEVELOPMENTAL, TAU)
values (1, 1, 10, 20, 'UNKNOWN', 'first', TRUE, .123),
       (2, 1, 30, 40, 'UNKNOWN', 'second', TRUE, .456);


truncate table SVANNA.ENHANCER_TISSUE_SPECIFICITY;

insert into SVANNA.ENHANCER_TISSUE_SPECIFICITY(ENHANCER_ID, TERM_ID, TERM_LABEL, HPO_ID, HPO_LABEL, SPECIFICITY)
values (1, 'UBERON:123', 'Head', 'HPO:111', 'Abnormality of some kind', .3),
       (1, 'UBERON:124', 'Toes', 'HPO:112', 'Abnormality of toes', .4),
       (2, 'UBERON:125', 'Finger', 'HPO:113', 'Abnormality of finger', .5);
