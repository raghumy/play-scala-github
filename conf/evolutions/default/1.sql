# DC schema

# --- !Ups

CREATE TABLE orgs (
    id integer NOT NULL PRIMARY KEY,
    name varchar(255) NOT NULL UNIQUE,
    last_updated integer,
    state varchar(255)
);

CREATE TABLE orgs_data (
    id integer NOT NULL PRIMARY KEY,
    org varchar(255) NOT NULL UNIQUE,
    members_json CLOB,
    repos_json CLOB,
    CONSTRAINT fk_org
        FOREIGN KEY (org)
        REFERENCES orgs(org)
        ON DELETE CASCADE
);

CREATE TABLE repos (
    id integer NOT NULL PRIMARY KEY,
    name varchar(255) NOT NULL,
    org varchar(255) NOT NULL,
    forks integer,
    last_updated integer,
    open_issues integer,
    stars integer,
    watchers integer,
    CONSTRAINT fk_org
        FOREIGN KEY (org)
        REFERENCES orgs(org)
        ON DELETE CASCADE
);

# --- !Downs

DROP TABLE repos;

DROP TABLE orgs;