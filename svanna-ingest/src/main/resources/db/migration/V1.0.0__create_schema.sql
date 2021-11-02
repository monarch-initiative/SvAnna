create schema if not exists SVANNA;

drop table if exists SVANNA.ENHANCERS;
create table SVANNA.ENHANCERS
(
    ENHANCER_ID      INT auto_increment,
    CONTIG           INT          not null,
    START            INT          not null, -- zero-based start on POSITIVE strand
    END              INT          not null, -- zero-based end on POSITIVE strand
    ENHANCER_SOURCE  VARCHAR(50)  not null,
    NAME             VARCHAR(255) not null,
    IS_DEVELOPMENTAL BOOL         not null,
    TAU              DOUBLE       not null
);

create index SVANNA.ENHANCERS__CONTIG_START_END_IDX
    on SVANNA.ENHANCERS (CONTIG, START, END);


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

drop table if exists SVANNA.REPETITIVE_REGIONS;
create table SVANNA.REPETITIVE_REGIONS
(
    CONTIG        INT         not null,
    START         INT         not null, -- zero-based start on POSITIVE strand
    END           INT         not null, -- zero-based end on POSITIVE strand
    REPEAT_FAMILY VARCHAR(50) not null
);
create
index SVANNA.REPETITIVE_REGIONS__CONTIG_START_END_IDX
    on SVANNA.REPETITIVE_REGIONS (CONTIG, START, END);

drop table if exists SVANNA.POPULATION_VARIANTS;
create table SVANNA.POPULATION_VARIANTS
(
    CONTIG           INT          not null,
    START            INT          not null, -- zero-based start on POSITIVE strand
    END              INT          not null, -- zero-based end on POSITIVE strand

    ID               VARCHAR(200) not null,
    VARIANT_TYPE     VARCHAR(20)  not null,
    ORIGIN           VARCHAR(40)  not null,
    ALLELE_FREQUENCY FLOAT        not null
);
create
index SVANNA.POPULATION_VARIANTS__CONTIG_START_END_IDX
    on SVANNA.POPULATION_VARIANTS (CONTIG, START, END);

drop table if exists SVANNA.TAD_BOUNDARY;
create table SVANNA.TAD_BOUNDARY
(
    CONTIG    INT          not null,
    START     INT          not null, -- zero-based start on POSITIVE strand
    END       INT          not null, -- zero-based end on POSITIVE strand
    MIDPOINT  INT          not null,
    ID        VARCHAR(200) not null,
    STABILITY FLOAT        not null
);

create index SVANNA.TAD_BOUNDARY__CONTIG_START_END_IDX
	on SVANNA.TAD_BOUNDARY (CONTIG, START, END);

create index SVANNA.TAD_BOUNDARY__CONTIG_MIDPOINT_IDX
on SVANNA.TAD_BOUNDARY (CONTIG, MIDPOINT);

-- TODO - these are not similarities but mere IC_{MICA} values
drop table if exists SVANNA.RESNIK_SIMILARITY;
create table SVANNA.RESNIK_SIMILARITY
(
    LEFT_ID    INT   not null, -- left term ID. The id for `HP:0001234` is 1234
    RIGHT_ID   INT   not null, -- right term ID
    SIMILARITY FLOAT not null  -- Resnik similarity value
);
create unique index SVANNA.RESNIK_SIMILARITY__IDX on SVANNA.RESNIK_SIMILARITY (LEFT_ID, RIGHT_ID);
