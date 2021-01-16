CREATE SCHEMA list;

CREATE TABLE list.NIMHDemographics(
    SubjectID INTEGER,
    Name VARCHAR(50),
    Family VARCHAR(30),
    Mother DECIMAL,
    Father DECIMAL,
    Species VARCHAR(25),
    Occupation VARCHAR(30),
    MaritalStatus VARCHAR(20),
    CurrentStatus VARCHAR(20),
    Gender VARCHAR,
    BirthDate DATE,
    Image BOOLEAN,

    CONSTRAINT PK_NIMHDemographics PRIMARY KEY (SubjectID)
);

CREATE TABLE list.NIMHSamples(
    SampleID BIGINT,
    SubjectID INTEGER,
    DateObtained DATE,
    SampleType VARCHAR,
    Volume REAL,
    VolumeDouble DOUBLE PRECISION,
    VolumeUnits CHAR(5),

    CONSTRAINT PK_NIMHSamples PRIMARY KEY (SampleID),
    CONSTRAINT FK_NIMHSamples_SubjectID FOREIGN KEY (SubjectID) REFERENCES list.NIMHDemographics (SubjectId)
);

CREATE TABLE list.NIMHPortions(
    PortionID SMALLINT,
    SubjectID INTEGER,
    SampleID BIGINT,
    DateSeparated TIMESTAMP,
    Division VARCHAR(10),

    CONSTRAINT PK_NIMHPortions PRIMARY KEY (PortionID),
    CONSTRAINT FK_NIMHPortions_SubjectID FOREIGN KEY (SubjectID) REFERENCES list.NIMHDemographics (SubjectId),
    CONSTRAINT FK_NIMHPortions_SampleID FOREIGN KEY (SampleID) REFERENCES list.NIMHSamples (SampleID)
);

CREATE TABLE list.NIMHSlides(
    SlideID INTEGER,
    SubjectID INTEGER,
    SampleID BIGINT,
    PortionID SMALLINT,
    StainPositive BOOLEAN,
    StainDate TIMESTAMP,
    StainTime TIME,

    CONSTRAINT PK_NIMHSlides PRIMARY KEY (SlideID),
    CONSTRAINT FK_NIMHSlides_SubjectID FOREIGN KEY (SubjectID) REFERENCES list.NIMHDemographics (SubjectId),
    CONSTRAINT FK_NIMHSlides_SampleID FOREIGN KEY (SampleID) REFERENCES list.NIMHSamples (SampleID),
    CONSTRAINT FK_NIMHSlides_PortionID FOREIGN KEY (PortionID) REFERENCES list.NIMHPortions (PortionID)
);
