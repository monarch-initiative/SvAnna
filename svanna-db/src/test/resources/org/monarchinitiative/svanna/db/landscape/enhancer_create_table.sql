create schema if not exists SVANNA;

drop table if exists SVANNA.ENHANCERS;
create table SVANNA.ENHANCERS
(
    ENHANCER_ID      INT auto_increment,
    CONTIG           INT          not null,
    START_POS        INT          not null,
    END_POS          INT          not null,
    ENHANCER_SOURCE  VARCHAR(50)  not null,
    NAME             VARCHAR(255) not null,
    IS_DEVELOPMENTAL BOOL         not null,
    TAU              DOUBLE       not null
);

create index SVANNA.ENHANCERS__CONTIG_START_END_IDX
    on SVANNA.ENHANCERS (CONTIG, START_POS, END_POS);


drop table if exists SVANNA.ENHANCER_TISSUE_SPECIFICITY;
create table SVANNA.ENHANCER_TISSUE_SPECIFICITY
(
    ENHANCER_ID INT          not null,
    TERM_ID     VARCHAR(50)  not null,
    TERM_LABEL  VARCHAR(500) not null,
    HPO_ID      VARCHAR(50)  not null,
    HPO_LABEL   VARCHAR(500) not null,
    SPECIFICITY DOUBLE       not null
);

create index SVANNA.ENHANCER_TISSUE_SPECIFICITY__ENHANCER_ID
    on SVANNA.ENHANCER_TISSUE_SPECIFICITY (ENHANCER_ID);

