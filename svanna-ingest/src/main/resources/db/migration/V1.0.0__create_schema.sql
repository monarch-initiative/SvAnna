create schema if not exists SVANNA;

drop table if exists SVANNA.ENHANCERS;
create table SVANNA.ENHANCERS
(
    ENHANCER_ID      INT          GENERATED ALWAYS AS IDENTITY,
    CONTIG           INT          not null,
    START_POS        INT          not null, -- zero-based start on POSITIVE strand
    END_POS          INT          not null, -- zero-based end on POSITIVE strand
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

---------------------------------- REPETITIVE REGIONS ------------------------------------------------------------------
drop table if exists SVANNA.REPETITIVE_REGIONS;
create table SVANNA.REPETITIVE_REGIONS
(
    CONTIG        INT         not null,
    START_POS     INT         not null, -- zero-based start on POSITIVE strand
    END_POS       INT         not null, -- zero-based end on POSITIVE strand
    REPEAT_FAMILY VARCHAR(50) not null
);
create
    index SVANNA.REPETITIVE_REGIONS__CONTIG_START_END_IDX
    on SVANNA.REPETITIVE_REGIONS (CONTIG, START_POS, END_POS);

---------------------------------- POPULATION VARIANTS -----------------------------------------------------------------
drop table if exists SVANNA.POPULATION_VARIANTS;
create table SVANNA.POPULATION_VARIANTS
(
    CONTIG           INT          not null,
    START_POS        INT          not null, -- zero-based start on POSITIVE strand
    END_POS          INT          not null, -- zero-based end on POSITIVE strand

    ID               VARCHAR(200) not null,
    VARIANT_TYPE     VARCHAR(20)  not null,
    ORIGIN           VARCHAR(40)  not null,
    ALLELE_FREQUENCY FLOAT        not null
);
create
    index SVANNA.POPULATION_VARIANTS__CONTIG_START_END_IDX
    on SVANNA.POPULATION_VARIANTS (CONTIG, START_POS, END_POS);

---------------------------------- TAD BOUNDARY ------------------------------------------------------------------------
drop table if exists SVANNA.TAD_BOUNDARY;
create table SVANNA.TAD_BOUNDARY
(
    CONTIG    INT          not null,
    START_POS INT          not null, -- zero-based start on POSITIVE strand
    END_POS   INT          not null, -- zero-based end on POSITIVE strand
    MIDPOINT  INT          not null,
    ID        VARCHAR(200) not null,
    STABILITY FLOAT        not null
);

create index SVANNA.TAD_BOUNDARY__CONTIG_START_END_IDX
    on SVANNA.TAD_BOUNDARY (CONTIG, START_POS, END_POS);

create index SVANNA.TAD_BOUNDARY__CONTIG_MIDPOINT_IDX
    on SVANNA.TAD_BOUNDARY (CONTIG, MIDPOINT);


---------------------------------- CLINGEN DOSAGE ELEMENT --------------------------------------------------------------
drop table if exists SVANNA.CLINGEN_DOSAGE_ELEMENT;
create table SVANNA.CLINGEN_DOSAGE_ELEMENT
(
    CONTIG             INT          not null,
    START_POS          INT          not null, -- zero-based start on POSITIVE strand
    END_POS            INT          not null, -- zero-based end on POSITIVE strand

    ID                 VARCHAR(200) not null, -- HGNC ID or other ID if available
    DOSAGE_SENSITIVITY VARCHAR(20)  not null,
    DOSAGE_EVIDENCE    VARCHAR(20)  not null
);
drop index if exists SVANNA.CLINGEN_DOSAGE_ELEMENT__CONTIG_START_END_IDX;
create index SVANNA.CLINGEN_DOSAGE_ELEMENT__CONTIG_START_END_IDX
    on SVANNA.CLINGEN_DOSAGE_ELEMENT (CONTIG, START_POS, END_POS);

-- TODO - we should update DA layer to use numeric IDs, where available
drop index if exists SVANNA.CLINGEN_DOSAGE_ELEMENT__ID;
create index SVANNA.CLINGEN_DOSAGE_ELEMENT__ID on SVANNA.CLINGEN_DOSAGE_ELEMENT (ID);
