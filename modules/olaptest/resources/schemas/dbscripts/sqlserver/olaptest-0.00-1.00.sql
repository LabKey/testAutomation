CREATE SCHEMA olaptest;

CREATE TABLE olaptest.Facts (
    Container ENTITYID NOT NULL,
    Name NVARCHAR(100) NOT NULL,
    Project NVARCHAR(100),
    Location NVARCHAR(100),
    Type NVARCHAR(100),
    Volume DOUBLE PRECISION,
    CONSTRAINT PK_Facts PRIMARY KEY (Name)
);
GO