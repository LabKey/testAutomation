CREATE TABLE vehicle.FirstFKTable
(
    RowId INT NOT NULL,
    StartCycleCol INT NOT NULL UNIQUE,
    CONSTRAINT PK_FirstFKTable PRIMARY KEY (RowId)
);

CREATE TABLE vehicle.SecondFKTable
(
    RowId INT NOT NULL,
    StartCycleCol INT NOT NULL UNIQUE,
    CycleCol INT NOT NULL UNIQUE,

    CONSTRAINT PK_SecondFKTable PRIMARY KEY (RowId)
);

CREATE TABLE vehicle.ThirdFKTable
(
    RowId INT NOT NULL,
    CycleCol INT NOT NULL UNIQUE,

    CONSTRAINT PK_ThirdFKTable PRIMARY KEY (RowId)
);

--ALTER TABLE vehicle.SecondFKTable ADD CONSTRAINT FK_FirstFKTable_StartCycleCol FOREIGN KEY (StartCycleCol) REFERENCES vehicle.FirstFKTable (StartCycleCol);
ALTER TABLE vehicle.FirstFKTable ADD CONSTRAINT FK_ThirdFKTable_StartCycleCol FOREIGN KEY (StartCycleCol) REFERENCES vehicle.SecondFKTable (StartCycleCol);
ALTER TABLE vehicle.SecondFKTable ADD CONSTRAINT FK_ThirdFKTable_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.ThirdFKTable (CycleCol);
ALTER TABLE vehicle.ThirdFKTable ADD CONSTRAINT FK_SecondFKTable_CycleCol FOREIGN KEY (CycleCol) REFERENCES vehicle.SecondFKTable (CycleCol);