create table if not exists applications
(
    app_id bigint generated by default as identity not null,
    app    varchar(20),
    constraint pk_app primary key (app_id),
    constraint uq_app unique (app)
    );

create table if not exists stats
(
    id_stats  bigint generated by default as identity not null,
    app_id    bigint,
    uri       varchar(256)                            not null,
    ip        varchar(15)                             not null,
    timestamp varchar(100)                            not null,
    constraint pk_stat primary key (id_stats),
    constraint fk_app foreign key (app_id) references applications (app_id)
    );