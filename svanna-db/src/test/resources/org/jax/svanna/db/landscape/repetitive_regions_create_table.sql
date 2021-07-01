create schema if not exists SVANNA;

drop table if exists SVANNA.REPETITIVE_REGIONS;
create table SVANNA.REPETITIVE_REGIONS
(
    ENHANCER_ID   INT auto_increment,
    CONTIG        INT         not null,
    START         INT         not null,
    END           INT         not null,
    REPEAT_FAMILY VARCHAR(50) not null
);
create index SVANNA.REPETITIVE_REGIONS__CONTIG_START_ON_POS_END_ON_POS_IDX
    on SVANNA.REPETITIVE_REGIONS (CONTIG, START, END);
