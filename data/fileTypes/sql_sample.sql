CREATE DOMAIN public.UNIQUEIDENTIFIER AS VARCHAR(36);
CREATE DOMAIN public.ENTITYID AS VARCHAR(36);
CREATE DOMAIN public.USERID AS INT;

CREATE SCHEMA core;
CREATE SCHEMA temp;
CREATE SCHEMA portal;

-- for JDBC Login support, validates email/password,
-- UserId is stored in the Principals table
-- LDAP authenticated users are not in this table

CREATE TABLE core.Logins
(
    Email VARCHAR(255) NOT NULL,
    Crypt VARCHAR(64) NOT NULL,
    Verification VARCHAR(64),
    LastChanged TIMESTAMP NULL,
    PreviousCrypts VARCHAR(1000),

    CONSTRAINT PK_Logins PRIMARY KEY (Email)
);

-- Principals is used for managing security related information
-- It is not used for validating login, that requires an 'external'
-- process, either using LDAP, JDBC, etc. (see Logins table)
--
-- It does not contain contact info or other generic user visible data

CREATE TABLE core.Principals
(
);

SELECT SETVAL('core.Principals_UserId_Seq', 1000);

-- maps users to groups
CREATE TABLE core.Members
(
);

CREATE TABLE core.UsersData
(
);

CREATE TABLE core.Containers
(
);

CREATE INDEX IX_Containers_Parent_Entity ON core.Containers(Parent, EntityId);

-- table for all modules
CREATE TABLE core.Modules
(
);

-- keep track of sql scripts that have been run in each module
CREATE TABLE core.SqlScripts
(
);

-- generic table for all attached docs
CREATE TABLE core.Documents
(
    -- standard fields
    _ts TIMESTAMP DEFAULT now(),
    RowId SERIAL,
    CreatedBy USERID,
    Created TIMESTAMP,
    ModifiedBy USERID,
    Modified TIMESTAMP,
    Owner USERID NULL,

    Container ENTITYID NOT NULL,    -- Container of parent, if parent has no ACLs
    Parent ENTITYID NOT NULL,
    DocumentName VARCHAR(195),    --filename

    DocumentSize INT DEFAULT -1,
    DocumentType VARCHAR(500) DEFAULT 'text/plain',    -- Needs to be large enough to handle new Office document mime-types
    Document BYTEA,            -- ContentType LIKE application/*

    LastIndexed TIMESTAMP NULL,

    CONSTRAINT PK_Documents PRIMARY KEY (RowId),
    CONSTRAINT UQ_Documents_Parent_DocumentName UNIQUE (Parent, DocumentName)
);

CREATE INDEX IX_Documents_Container ON core.Documents(Container);
CREATE INDEX IX_Documents_Parent ON core.Documents(Parent);

