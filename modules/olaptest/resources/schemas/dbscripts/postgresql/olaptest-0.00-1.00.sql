
CREATE SCHEMA olaptest;

CREATE TABLE olaptest.Facts (
    Container ENTITYID NOT NULL,
    Name VARCHAR(100) NOT NULL,
    Project VARCHAR(100),
    Location VARCHAR(100),
    Type VARCHAR(100),
    Volume DOUBLE PRECISION,
    CONSTRAINT PK_Facts PRIMARY KEY (Name)
);