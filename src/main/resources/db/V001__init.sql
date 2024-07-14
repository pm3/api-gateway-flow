create table flow_case (

    id varchar(32) not null primary key,
    tenant varchar(64) not null,
    caseType varchar(32) not null,
    externalId varchar(128),
    callback text,
    params text,
    assets text,
    response text,
    created timestamp not null,
    finished timestamp,
    state varchar(32) not null
);

create table flow_task (
    id varchar(32) not null primary key,
    flowCaseId  varchar(32) not null,
    step varchar(64) not null,
    worker varchar(64) not null,
    assetId varchar(32),
    response text,
    error text,
    created timestamp not null,
    started timestamp,
    finished timestamp
);

create index flow_task_case_id on flow_task(flowCaseId);